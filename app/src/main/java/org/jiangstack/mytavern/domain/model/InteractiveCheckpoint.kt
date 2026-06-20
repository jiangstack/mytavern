package org.jiangstack.mytavern.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class InteractiveCheckpoint(
    val id: Long = 0,
    val gameId: Long,
    val parentId: Long? = null,
    val name: String,
    val snapshot: InteractiveCheckpointSnapshot,
    val createdAt: Long = System.currentTimeMillis()
)
