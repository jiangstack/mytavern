package org.jiangstack.mytavern.domain.model

data class ChatMessage(
    val id: Long = 0,
    val sessionId: Long,
    val senderId: Long? = null,
    val senderName: String? = null,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val parentMessageId: Long? = null
)
