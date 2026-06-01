package org.jiangstack.mytavern.domain.model

data class NovelChapter(
    val id: Long = 0,
    val novelId: Long,
    val chapterNumber: Int,
    val title: String,
    val outline: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
