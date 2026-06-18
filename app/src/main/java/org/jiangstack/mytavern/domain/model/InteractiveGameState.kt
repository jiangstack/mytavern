package org.jiangstack.mytavern.domain.model

data class InteractiveGameState(
    val gameId: Long,
    val environment: String = "",
    val characterStatus: String = "",
    val characterItems: String = ""
)
