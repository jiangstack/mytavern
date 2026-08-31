package org.jiangstack.mytavern.domain.model

import kotlinx.serialization.Serializable

/**
 * LLM 展开的世界事件方案。
 */
@Serializable
data class ScheduleChange(
    val memberId: Long,
    val items: List<ScheduleItem> = emptyList()
)

@Serializable
data class EventPlan(
    val title: String,
    val description: String = "",
    val locationId: Long? = null,
    val participantIds: List<Long> = emptyList(),
    val scheduleChanges: List<ScheduleChange> = emptyList()
)
