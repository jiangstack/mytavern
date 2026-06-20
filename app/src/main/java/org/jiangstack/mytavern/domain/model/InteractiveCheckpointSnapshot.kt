package org.jiangstack.mytavern.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class InteractiveCheckpointSnapshot(
    val messages: List<InteractiveMessage>,
    val gameState: InteractiveGameState?
)
