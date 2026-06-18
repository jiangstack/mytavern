package org.jiangstack.mytavern.domain.model

data class InteractiveGame(
    val id: Long = 0,
    val title: String,
    val narratorStyle: String = "",
    val storyBackground: String = "",
    val storyMainPlot: String = "",
    val windowWordCount: Int = 3000,
    val playCharacterId: Long,
    val worldBookId: Long? = null,
    val characterIds: List<Long> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
