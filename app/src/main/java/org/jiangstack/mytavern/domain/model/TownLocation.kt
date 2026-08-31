package org.jiangstack.mytavern.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TownLocation(
    val id: Long = 0,
    val townId: Long,
    val name: String,
    val description: String = ""
)
