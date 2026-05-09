package org.jiangstack.mytavern.domain.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.jiangstack.mytavern.data.remote.AnthropicMessage
import org.jiangstack.mytavern.data.remote.AnthropicRequest
import org.jiangstack.mytavern.data.remote.ChatCompletionRequest
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
    private val userPreferencesRepository: UserPreferencesRepository
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
                        authorization = "Bearer ${activeConfig.apiKey}",
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
                        apiKey = activeConfig.apiKey,
                        request = request
                    ).content?.firstOrNull()?.text
                }
            }

            Result.success(response ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getDefaultConfig(): LlmConfig? {
        val defaultId = userPreferencesRepository.defaultLlmConfigId.first()
        return defaultId?.let { llmConfigRepository.getConfigById(it) }
    }
}
