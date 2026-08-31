package org.jiangstack.mytavern.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TownMember(
    val id: Long = 0,
    val townId: Long,
    val characterId: Long,
    val persona: String = "",
    val isPlayerControlled: Boolean = false,
    val currentLocationId: Long? = null,
    val currentActivity: String = "",
    val mood: String = "",
    val todaySchedule: List<ScheduleItem> = emptyList(),
    val recentMemory: List<TownMemoryEntry> = emptyList(),
    val importantMemory: List<TownMemoryEntry> = emptyList()
)

/**
 * 一段日程：[startHour, endHour) 小时区间，在指定地点做某事。
 */
@Serializable
data class ScheduleItem(
    val startHour: Int,
    val endHour: Int,
    val locationId: Long,
    val activity: String
)

@Serializable
data class TownMemoryEntry(
    val content: String,
    val day: Int,
    val hour: Int
)
