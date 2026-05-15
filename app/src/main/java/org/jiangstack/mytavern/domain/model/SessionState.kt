package org.jiangstack.mytavern.domain.model

data class SessionState(
    val sessionId: Long,
    val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)
