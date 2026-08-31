package org.jiangstack.mytavern

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.jiangstack.mytavern.domain.model.ScheduleItem
import org.jiangstack.mytavern.domain.service.TownScheduleValidator

class TownScheduleValidatorTest {

    private val locations = setOf(1L, 2L, 3L)

    @Test
    fun `parses valid json and maps invalid location to default`() {
        val json = """[{"startHour":8,"endHour":12,"locationId":1,"activity":"劳作"},{"startHour":12,"endHour":18,"locationId":99,"activity":"闲逛"}]"""
        val items = TownScheduleValidator.sanitize(json, locations, defaultLocationId = 3L)
        assertEquals(1L, items.first { it.startHour == 8 }.locationId)
        // 无效地点被映射为默认地点，且空隙被补齐后仍全时段覆盖
        assertTrue(items.all { it.locationId in locations })
    }

    @Test
    fun `malformed json falls back to full default coverage`() {
        val items = TownScheduleValidator.sanitize("not a json", locations, defaultLocationId = 2L)
        assertEquals(1, items.size)
        assertEquals(0, items[0].startHour)
        assertEquals(24, items[0].endHour)
        assertEquals(2L, items[0].locationId)
    }

    @Test
    fun `fills gaps so that coverage is zero to twenty four`() {
        val items = TownScheduleValidator.sanitizeItems(
            listOf(ScheduleItem(9, 12, 1, "干活"), ScheduleItem(20, 23, 2, "喝酒")),
            locations, defaultLocationId = 3L
        )
        assertEquals(0, items.first().startHour)
        assertEquals(24, items.last().endHour)
        // 相邻条目无缝衔接
        items.zipWithNext().forEach { (a, b) ->
            assertEquals(a.endHour, b.startHour)
        }
    }

    @Test
    fun `overlapping items keep earlier one in overlap and truncate later`() {
        val items = TownScheduleValidator.sanitizeItems(
            listOf(
                ScheduleItem(8, 16, 1, "上午到下午"),
                ScheduleItem(12, 20, 2, "中午到晚上")
            ),
            locations, defaultLocationId = 1L
        )
        // 前一条截断为8-12（保留非重叠头部），第二条12-20完整保留，头部尾部补齐
        assertEquals(4, items.size)
        assertEquals(0, items[0].startHour)
        assertEquals(8, items[1].startHour)
        assertEquals(12, items[1].endHour)
        assertEquals(1L, items[1].locationId)
        assertEquals(12, items[2].startHour)
        assertEquals(20, items[2].endHour)
        assertEquals(2L, items[2].locationId)
        assertEquals(24, items[3].endHour)
    }

    @Test
    fun `mergeChanges removes overlapping old items`() {
        val base = listOf(
            ScheduleItem(0, 8, 1, "休息"),
            ScheduleItem(8, 12, 2, "田里干活"),
            ScheduleItem(12, 24, 1, "休息")
        )
        val merged = TownScheduleValidator.mergeChanges(
            base,
            listOf(ScheduleItem(9, 11, 3, "围观事件")),
            locations, defaultLocationId = 1L
        )
        // 8-12 被事件拆分：9-11 在地点3
        assertTrue(merged.any { it.startHour == 9 && it.endHour == 11 && it.locationId == 3L })
        assertTrue(merged.none { it.startHour == 8 && it.endHour == 12 && it.locationId == 2L })
        // 全覆盖
        assertEquals(0, merged.first().startHour)
        assertEquals(24, merged.last().endHour)
    }

    @Test
    fun `hours are coerced into range`() {
        val items = TownScheduleValidator.sanitizeItems(
            listOf(ScheduleItem(-3, 30, 1, "全天")),
            locations, defaultLocationId = 1L
        )
        assertEquals(1, items.size)
        assertEquals(0, items[0].startHour)
        assertEquals(24, items[0].endHour)
    }
}
