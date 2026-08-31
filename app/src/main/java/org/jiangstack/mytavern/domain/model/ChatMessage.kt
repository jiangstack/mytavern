package org.jiangstack.mytavern.domain.model

import org.jiangstack.mytavern.data.remote.ToolCall

data class ChatMessage(
    val id: Long = 0,
    val sessionId: Long,
    val senderId: Long? = null,
    val senderName: String? = null,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val parentMessageId: Long? = null,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null,
    val role: String? = null,
    val messageType: String? = null,
    /** 工具调用协议：assistant 消息携带的工具调用列表（仅内存中传递给 LLM，不落库） */
    val toolCalls: List<ToolCall>? = null,
    /** 工具调用协议：tool 消息对应的调用 ID（仅内存中传递给 LLM，不落库） */
    val toolCallId: String? = null
)
