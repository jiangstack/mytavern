package org.jiangstack.mytavern.domain.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jiangstack.mytavern.data.remote.AnthropicMessage
import org.jiangstack.mytavern.data.remote.AnthropicRequest
import org.jiangstack.mytavern.data.remote.ChatCompletionRequest
import org.jiangstack.mytavern.data.remote.ChatCompletionStreamResponse
import org.jiangstack.mytavern.data.remote.LlmApiService
import org.jiangstack.mytavern.data.remote.Message
import org.jiangstack.mytavern.domain.model.ApiType
import org.jiangstack.mytavern.domain.model.ChatMessage
import org.jiangstack.mytavern.domain.model.LlmConfig
import org.jiangstack.mytavern.domain.repository.LlmConfigRepository
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository

class LlmService(
    private val llmApiService: LlmApiService,
    private val llmConfigRepository: LlmConfigRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {

    suspend fun sendChatMessage(
        messages: List<ChatMessage>,
        systemPrompt: String,
        config: LlmConfig? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val activeConfig = config ?: getDefaultConfig()
                ?: return@withContext Result.failure(IllegalStateException("没有可用的 LLM 配置"))

            val response = when (activeConfig.apiType) {
                ApiType.OPENAI, ApiType.OPENRESPONSES -> {
                    val requestMessages = mutableListOf(Message("system", systemPrompt))
                    requestMessages.addAll(
                        messages.map {
                            Message(
                                role = if (it.senderId == null) "user" else "assistant",
                                content = it.content
                            )
                        }
                    )
                    val request = ChatCompletionRequest(
                        model = activeConfig.model,
                        messages = requestMessages
                    )
                    llmApiService.chatCompletion(
                        url = activeConfig.baseUrl,
                        authorization = activeConfig.apiKey.takeIf { it.isNotBlank() }?.let { "Bearer $it" },
                        request = request
                    ).choices?.firstOrNull()?.message?.content
                }

                ApiType.ANTHROPIC -> {
                    val anthropicMessages = messages.map {
                        AnthropicMessage(
                            role = if (it.senderId == null) "user" else "assistant",
                            content = it.content
                        )
                    }
                    val request = AnthropicRequest(
                        model = activeConfig.model,
                        messages = anthropicMessages,
                        system = systemPrompt
                    )
                    llmApiService.chatCompletionAnthropic(
                        url = activeConfig.baseUrl,
                        apiKey = activeConfig.apiKey.takeIf { it.isNotBlank() },
                        request = request
                    ).content?.firstOrNull()?.text
                }
            }

            Result.success(response ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sendChatMessageStream(
        messages: List<ChatMessage>,
        systemPrompt: String,
        config: LlmConfig? = null
    ): Flow<StreamChunk> = flow {
        val activeConfig = config ?: getDefaultConfig()
            ?: throw IllegalStateException("没有可用的 LLM 配置")

        when (activeConfig.apiType) {
            ApiType.OPENAI, ApiType.OPENRESPONSES -> {
                val requestMessages = mutableListOf(Message("system", systemPrompt))
                requestMessages.addAll(
                    messages.map {
                        Message(
                            role = if (it.senderId == null) "user" else "assistant",
                            content = it.content
                        )
                    }
                )
                val chatRequest = ChatCompletionRequest(
                    model = activeConfig.model,
                    messages = requestMessages,
                    stream = true
                )
                val requestBody = json.encodeToString(
                    ChatCompletionRequest.serializer(),
                    chatRequest
                ).toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(activeConfig.baseUrl)
                    .post(requestBody)
                    .apply {
                        if (activeConfig.apiKey.isNotBlank()) {
                            addHeader("Authorization", "Bearer ${activeConfig.apiKey}")
                        }
                    }
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "text/event-stream")
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw java.io.IOException("HTTP ${response.code}")
                    }
                    response.body?.source()?.use { source ->
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            if (!line.startsWith("data: ")) continue
                            val data = line.removePrefix("data: ").trim()
                            if (data == "[DONE]") break
                            if (data.isBlank()) continue
                            try {
                                val streamResponse = json.decodeFromString(
                                    ChatCompletionStreamResponse.serializer(),
                                    data
                                )
                                val delta = streamResponse.choices?.firstOrNull()?.delta
                                val content = delta?.content ?: ""
                                val reasoning = delta?.reasoning_content ?: ""
                                if (content.isNotBlank() || reasoning.isNotBlank()) {
                                    emit(StreamChunk(content = content, reasoningContent = reasoning))
                                }
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
            }

            ApiType.ANTHROPIC -> {
                throw IllegalStateException("Anthropic 流式输出暂不支持")
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun getDefaultConfig(): LlmConfig? {
        val defaultId = userPreferencesRepository.defaultLlmConfigId.first()
        return defaultId?.let { llmConfigRepository.getConfigById(it) }
    }
}
