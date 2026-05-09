package org.jiangstack.mytavern.domain.model

data class Character(
    val id: Long = 0,
    val name: String,
    val description: String,
    val avatarUri: String? = null
)
