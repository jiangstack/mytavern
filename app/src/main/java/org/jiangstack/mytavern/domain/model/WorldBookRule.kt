package org.jiangstack.mytavern.domain.model

data class WorldBookRule(
    val id: Long = 0,
    val worldBookId: Long,
    val name: String,
    val description: String,
    val matchType: String
)
