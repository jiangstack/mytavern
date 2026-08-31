package org.jiangstack.mytavern.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Town(
    val id: Long = 0,
    val name: String,
    val worldDescription: String = "",
    val currentDay: Int = 1,
    val currentHour: Int = 8,
    val playMemberId: Long? = null,
    val windowWordCount: Int = 3000,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
