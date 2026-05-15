package org.jiangstack.mytavern.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "session_states",
    primaryKeys = ["sessionId", "stateKey"],
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SessionStateEntity(
    val sessionId: Long,
    val stateKey: String,
    val stateValue: String,
    val updatedAt: Long = System.currentTimeMillis()
)
