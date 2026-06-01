package org.jiangstack.mytavern.domain.model

data class Novel(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val worldBookId: Long? = null,
    val characterIds: List<Long> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
