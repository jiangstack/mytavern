package org.jiangstack.mytavern.domain.model

data class InteractiveMessage(
    val id: Long = 0,
    val gameId: Long,
    val role: String,
    val content: String,
    val actionOptions: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
