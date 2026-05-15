package org.jiangstack.mytavern.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val title: String,
    val backgroundUri: String? = null,
    val userCharacterId: Long? = null,
    val aiCharacterId: Long? = null,
    val worldBookId: Long? = null,
    val sessionStateEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
