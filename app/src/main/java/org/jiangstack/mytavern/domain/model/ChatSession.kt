package org.jiangstack.mytavern.domain.model

data class ChatSession(
    val id: Long = 0,
    val type: SessionType,
    val title: String,
    val backgroundUri: String? = null,
    val userCharacterId: Long? = null,
    val aiCharacterId: Long? = null,
    val worldBookId: Long? = null,
    val sessionStateEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SessionType {
    SINGLE,
    GROUP
}
