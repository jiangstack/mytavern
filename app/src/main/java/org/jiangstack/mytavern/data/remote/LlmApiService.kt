package org.jiangstack.mytavern.data.remote

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

interface LlmApiService {

    @POST
    suspend fun chatCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String? = null,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse

    @Streaming
    @POST
    suspend fun chatCompletionStream(
        @Url url: String,
        @Header("Authorization") authorization: String? = null,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: ChatCompletionRequest
    ): ResponseBody

    @POST
    suspend fun chatCompletionAnthropic(
        @Url url: String,
        @Header("x-api-key") apiKey: String? = null,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: AnthropicRequest
    ): AnthropicResponse
}

@kotlinx.serialization.Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false,
    val temperature: Float? = null,
    val reasoning: Reasoning? = null,
    val stream_options: StreamOptions? = null,
    val tools: List<Tool>? = null,
    val tool_choice: String? = null
)

@kotlinx.serialization.Serializable
data class StreamOptions(
    val include_usage: Boolean = true
)

@kotlinx.serialization.Serializable
data class Reasoning(
    val effort: String,
    val summary: String? = null
)

@kotlinx.serialization.Serializable
data class Message(
    val role: String,
    val content: String? = null,
    val tool_calls: List<ToolCall>? = null,
    val tool_call_id: String? = null,
    val name: String? = null
)

@kotlinx.serialization.Serializable
data class ChatCompletionResponse(
    val id: String? = null,
    val choices: List<Choice>? = null,
    val usage: Usage? = null
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
    val max_tokens: Int = 2048,
    val temperature: Float? = null,
    val system: String? = null
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

@kotlinx.serialization.Serializable
data class ChatCompletionStreamResponse(
    val id: String? = null,
    val choices: List<StreamChoice>? = null,
    val usage: Usage? = null
)

@kotlinx.serialization.Serializable
data class Usage(
    val prompt_tokens: Int? = null,
    val completion_tokens: Int? = null,
    val total_tokens: Int? = null
)

@kotlinx.serialization.Serializable
data class StreamChoice(
    val delta: Delta? = null,
    val index: Int? = null,
    val finish_reason: String? = null
)

@kotlinx.serialization.Serializable
data class Delta(
    val role: String? = null,
    val content: String? = null,
    val reasoning_content: String? = null,
    val reasoning: String? = null,
    val tool_calls: List<ToolCallDelta>? = null
)

@kotlinx.serialization.Serializable
data class Tool(
    val type: String = "function",
    val function: ToolFunction
)

@kotlinx.serialization.Serializable
data class ToolFunction(
    val name: String,
    val description: String,
    val parameters: ToolParameters
)

@kotlinx.serialization.Serializable
data class ToolParameters(
    val type: String = "object",
    val properties: Map<String, ToolProperty>,
    val required: List<String>
)

@kotlinx.serialization.Serializable
data class ToolProperty(
    val type: String,
    val description: String
)

@kotlinx.serialization.Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunction
)

@kotlinx.serialization.Serializable
data class ToolCallFunction(
    val name: String,
    val arguments: String
)

@kotlinx.serialization.Serializable
data class ToolCallDelta(
    val index: Int,
    val id: String? = null,
    val type: String? = null,
    val function: ToolCallFunctionDelta? = null
)

@kotlinx.serialization.Serializable
data class ToolCallFunctionDelta(
    val name: String? = null,
    val arguments: String? = null
)
