package org.jiangstack.mytavern.domain.model

data class WorldBook(
    val id: Long = 0,
    val name: String,
    val description: String,
    val rules: List<WorldBookRule> = emptyList()
)
