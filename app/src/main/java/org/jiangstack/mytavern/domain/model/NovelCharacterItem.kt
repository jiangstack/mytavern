package org.jiangstack.mytavern.domain.model

data class NovelCharacterItem(
    val id: Long = 0,
    val novelId: Long,
    val characterId: Long,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
