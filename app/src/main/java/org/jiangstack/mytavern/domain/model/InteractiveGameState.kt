package org.jiangstack.mytavern.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class InteractiveGameState(
    val gameId: Long,
    val environment: String = "",
    val characterStatus: String = "",
    val characterItems: String = "",
    val activeCheckpointId: Long? = null
)
