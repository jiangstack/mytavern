package org.jiangstack.mytavern.domain.service

import android.util.Log
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
import org.jiangstack.mytavern.data.remote.Reasoning
import org.jiangstack.mytavern.data.remote.StreamOptions
import org.jiangstack.mytavern.data.remote.Tool
import org.jiangstack.mytavern.data.remote.ToolCall
import org.jiangstack.mytavern.data.remote.ToolCallDelta
import org.jiangstack.mytavern.data.remote.ToolCallFunction
import org.jiangstack.mytavern.data.remote.ToolCallFunctionDelta
import org.jiangstack.mytavern.data.remote.ToolFunction
import org.jiangstack.mytavern.data.remote.ToolParameters
import org.jiangstack.mytavern.data.remote.ToolProperty
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

    companion object {
        val rememberStateTool = Tool(
            function = ToolFunction(
                name = "remember_session_state",
                description = "记录或更新当前会话中角色的状态。当角色的状态发生变化时（如心情改变、移动位置、关系进展等），调用此工具记录新的状态。",
                parameters = ToolParameters(
                    properties = mapOf(
                        "state_key" to ToolProperty("string", "状态名称，由角色名和状态名组成，如'张三心情'、'李四位置'、'张三与李四关系'等"),
                        "state_value" to ToolProperty("string", "状态的详细描述")
                    ),
                    required = listOf("state_key", "state_value")
                )
            )
        )
    }

    suspend fun sendChatMessage(
        messages: List<ChatMessage>,
        systemPrompt: String,
        config: LlmConfig? = null,
        thinkingEnabled: Boolean = true,
        isGroupChat: Boolean = false,
        temperature: Float? = null,
        tools: List<Tool>? = null,
        userName: String? = null
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
                                content = formatMessageContent(it, userName)
                            )
                        }
                    )
                    val request = ChatCompletionRequest(
                        model = activeConfig.model,
                        messages = requestMessages,
                        temperature = temperature,
                        reasoning = if (thinkingEnabled) null else Reasoning(effort = "none"),
                        tools = tools?.takeIf { activeConfig.apiType != ApiType.ANTHROPIC },
                        tool_choice = if (tools != null && activeConfig.apiType != ApiType.ANTHROPIC) "auto" else null
                    )
                    Log.d("LlmService", "非流式发送给 LLM 的请求: ${json.encodeToString(ChatCompletionRequest.serializer(), request)}")
                    val result = llmApiService.chatCompletion(
                        url = activeConfig.baseUrl,
                        authorization = activeConfig.apiKey.takeIf { it.isNotBlank() }?.let { "Bearer $it" },
                        request = request
                    )
                    Log.d("LlmService", "非流式 LLM 响应: $result")
                    result.choices?.firstOrNull()?.message?.content
                }

                ApiType.ANTHROPIC -> {
                    val anthropicMessages = messages.map {
                        AnthropicMessage(
                            role = if (it.senderId == null) "user" else "assistant",
                            content = formatMessageContent(it, userName)
                        )
                    }
                    val request = AnthropicRequest(
                        model = activeConfig.model,
                        messages = anthropicMessages,
                        temperature = temperature,
                        system = systemPrompt
                    )
                    Log.d("LlmService", "Anthropic 发送给 LLM 的请求: ${json.encodeToString(AnthropicRequest.serializer(), request)}")
                    val result = llmApiService.chatCompletionAnthropic(
                        url = activeConfig.baseUrl,
                        apiKey = activeConfig.apiKey.takeIf { it.isNotBlank() },
                        request = request
                    )
                    Log.d("LlmService", "Anthropic LLM 响应: $result")
                    result.content?.firstOrNull()?.text
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
        config: LlmConfig? = null,
        thinkingEnabled: Boolean = true,
        isGroupChat: Boolean = false,
        temperature: Float? = null,
        tools: List<Tool>? = null,
        userName: String? = null
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
                            content = formatMessageContent(it, userName)
                        )
                    }
                )
                val chatRequest = ChatCompletionRequest(
                    model = activeConfig.model,
                    messages = requestMessages,
                    stream = true,
                    temperature = temperature,
                    reasoning = if (thinkingEnabled) null else Reasoning(effort = "none"),
                    stream_options = StreamOptions(include_usage = true),
                    tools = tools?.takeIf { activeConfig.apiType != ApiType.ANTHROPIC },
                    tool_choice = if (tools != null && activeConfig.apiType != ApiType.ANTHROPIC) "auto" else null
                )
                val requestBodyString = json.encodeToString(
                    ChatCompletionRequest.serializer(),
                    chatRequest
                )
                Log.d("LlmService", "发送给 LLM 的请求: $requestBodyString")
                val requestBody = requestBodyString.toRequestBody("application/json".toMediaType())

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
                        val errorBody = response.body?.string() ?: ""
                        Log.e("LlmService", "LLM 请求失败: HTTP ${response.code}, 响应: $errorBody")
                        throw java.io.IOException("HTTP ${response.code}")
                    }
                    Log.d("LlmService", "LLM 开始返回流式响应")
                    val fullResponse = StringBuilder()
                    val toolCallAccumulators = mutableMapOf<Int, ToolCallAccumulator>()
                    response.body?.source()?.use { source ->
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            fullResponse.appendLine(line)
                            if (!line.startsWith("data: ")) continue
                            val data = line.removePrefix("data: ").trim()
                            if (data == "[DONE]") {
                                Log.d("LlmService", "LLM 响应完成")
                                break
                            }
                            if (data.isBlank()) continue
                            try {
                                val streamResponse = json.decodeFromString(
                                    ChatCompletionStreamResponse.serializer(),
                                    data
                                )
                                Log.d("LlmService", "LLM 收到数据块: $data")
                                val delta = streamResponse.choices?.firstOrNull()?.delta
                                val content = delta?.content ?: ""
                                val reasoning = delta?.reasoning_content ?: delta?.reasoning ?: ""

                                delta?.tool_calls?.forEach { toolCallDelta ->
                                    val acc = toolCallAccumulators.getOrPut(toolCallDelta.index) { ToolCallAccumulator() }
                                    toolCallDelta.id?.let { acc.id = it }
                                    toolCallDelta.type?.let { acc.type = it }
                                    toolCallDelta.function?.name?.let { acc.name = it }
                                    toolCallDelta.function?.arguments?.let { acc.arguments.append(it) }
                                }

                                if (content.isNotBlank() || reasoning.isNotBlank()) {
                                    emit(StreamChunk(content = content, reasoningContent = reasoning))
                                }

                                val finishReason = streamResponse.choices?.firstOrNull()?.finish_reason
                                if (finishReason == "tool_calls" && toolCallAccumulators.isNotEmpty()) {
                                    val toolCalls = toolCallAccumulators.toSortedMap().map { (_, acc) ->
                                        ToolCall(
                                            id = acc.id ?: "",
                                            type = acc.type ?: "function",
                                            function = ToolCallFunction(
                                                name = acc.name ?: "",
                                                arguments = acc.arguments.toString()
                                            )
                                        )
                                    }
                                    emit(StreamChunk(toolCalls = toolCalls))
                                }

                                streamResponse.usage?.let { usage ->
                                    emit(StreamChunk(content = "", reasoningContent = "", usage = usage))
                                }
                            } catch (_: Exception) {
                            }
                        }
                        Log.d("LlmService", "LLM 完整响应: ${fullResponse.toString()}")
                    }
                }
            }

            ApiType.ANTHROPIC -> {
                throw IllegalStateException("Anthropic 流式输出暂不支持")
            }
        }
    }.flowOn(Dispatchers.IO)

    private data class ToolCallAccumulator(
        var id: String? = null,
        var type: String? = null,
        var name: String? = null,
        val arguments: StringBuilder = StringBuilder()
    )

    private fun formatMessageContent(message: ChatMessage, userName: String? = null): String {
        val prefix = when {
            message.senderId == null -> userName ?: "用户"
            else -> message.senderName ?: "AI"
        }
        if (message.content.startsWith(prefix)) {
            return message.content
        }
        return "$prefix：${message.content}"
    }

    private suspend fun getDefaultConfig(): LlmConfig? {
        val defaultId = userPreferencesRepository.defaultLlmConfigId.first()
        return defaultId?.let { llmConfigRepository.getConfigById(it) }
    }
}
