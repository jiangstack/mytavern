package org.jiangstack.mytavern.domain.service

import kotlinx.serialization.json.Json
import org.jiangstack.mytavern.domain.model.ScheduleItem

/**
 * 行动表清洗：容错解析 LLM 输出，校验时段与地点，重叠区间归先开始者（后一条截断起点），并保证 0-24 小时全覆盖。
 */
object TownScheduleValidator {
    private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

    fun sanitize(rawJson: String, validLocationIds: Set<Long>, defaultLocationId: Long): List<ScheduleItem> {
        val items = try {
            lenientJson.decodeFromString<List<ScheduleItem>>(rawJson)
        } catch (_: Exception) {
            emptyList()
        }
        return sanitizeItems(items, validLocationIds, defaultLocationId)
    }

    fun sanitizeItems(
        items: List<ScheduleItem>,
        validLocationIds: Set<Long>,
        defaultLocationId: Long
    ): List<ScheduleItem> {
        val cleaned = items.asSequence()
            .map { it.copy(startHour = it.startHour.coerceIn(0, 24), endHour = it.endHour.coerceIn(0, 24)) }
            .filter { it.endHour > it.startHour }
            .map { if (it.locationId in validLocationIds) it else it.copy(locationId = defaultLocationId) }
            .sortedBy { it.startHour }
            .toList()

        val merged = mutableListOf<ScheduleItem>()
        for (item in cleaned) {
            val last = merged.lastOrNull()
            when {
                last == null || item.startHour >= last.endHour -> merged.add(item)
                // 与上一条重叠且延伸更远：上一条截断到本条起点，保留其非重叠头部，再追加本条
                item.endHour > last.endHour -> {
                    merged[merged.lastIndex] = last.copy(endHour = item.startHour)
                    merged.add(item)
                }
                // else：完全被上一条覆盖，丢弃
            }
        }

        val full = mutableListOf<ScheduleItem>()
        var cursor = 0
        for (item in merged) {
            if (item.startHour > cursor) {
                full.add(
                    ScheduleItem(
                        startHour = cursor,
                        endHour = item.startHour,
                        locationId = full.lastOrNull()?.locationId ?: defaultLocationId,
                        activity = "日常琐事"
                    )
                )
            }
            full.add(item)
            cursor = item.endHour
        }
        if (cursor < 24) {
            full.add(
                ScheduleItem(
                    startHour = cursor,
                    endHour = 24,
                    locationId = full.lastOrNull()?.locationId ?: defaultLocationId,
                    activity = "日常琐事"
                )
            )
        }
        return full
    }

    /**
     * 世界事件改写日程：移除与新增条目重叠的旧条目后整体重新清洗。
     */
    fun mergeChanges(
        base: List<ScheduleItem>,
        changes: List<ScheduleItem>,
        validLocationIds: Set<Long>,
        defaultLocationId: Long
    ): List<ScheduleItem> {
        if (changes.isEmpty()) return base
        val kept = base.filter { item ->
            changes.none { change -> change.startHour < item.endHour && item.startHour < change.endHour }
        }
        return sanitizeItems(kept + changes, validLocationIds, defaultLocationId)
    }
}
