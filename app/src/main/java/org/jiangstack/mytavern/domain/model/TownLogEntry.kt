package org.jiangstack.mytavern.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TownLogEntry(
    val id: Long = 0,
    val townId: Long,
    val day: Int,
    val hour: Int,
    val kind: LogKind = LogKind.TICK,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class LogKind { TICK, EVENT, SYSTEM }
