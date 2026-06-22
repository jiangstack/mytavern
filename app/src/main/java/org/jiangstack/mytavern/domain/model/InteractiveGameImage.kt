package org.jiangstack.mytavern.domain.model

data class InteractiveGameImage(
    val id: Long = 0,
    val gameId: Long,
    val remoteUrl: String,
    val localUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
