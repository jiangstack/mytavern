package org.jiangstack.mytavern.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface LlmApiService {

    @POST
    suspend fun chatCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse

    @POST
    suspend fun chatCompletionAnthropic(
        @Url url: String,
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: AnthropicRequest
    ): AnthropicResponse
}

@kotlinx.serialization.Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false
)

@kotlinx.serialization.Serializable
data class Message(
    val role: String,
    val content: String
)

@kotlinx.serialization.Serializable
data class ChatCompletionResponse(
    val id: String? = null,
    val choices: List<Choice>? = null
)

@kotlinx.serialization.Serializable
data class Choice(
    val message: Message? = null,
    val index: Int? = null,
    val finish_reason: String? = null
)

@kotlinx.serialization.Serializable
data class AnthropicRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    val max_tokens: Int = 2048
)

@kotlinx.serialization.Serializable
data class AnthropicMessage(
    val role: String,
    val content: String
)

@kotlinx.serialization.Serializable
data class AnthropicResponse(
    val id: String? = null,
    val content: List<AnthropicContent>? = null
)

@kotlinx.serialization.Serializable
data class AnthropicContent(
    val type: String? = null,
    val text: String? = null
)
