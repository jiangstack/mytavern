package org.jiangstack.mytavern

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.jiangstack.mytavern.domain.service.TownSceneParser

class TownSceneParserTest {

    private val nameToId = mapOf("张三" to 1L, "李四" to 2L)

    @Test
    fun `parse dialogue line with speaker name`() {
        val line = TownSceneParser.parseLine("【对话|张三】今天天气不错。", nameToId)
        assertEquals("dialogue", line?.kind)
        assertEquals(1L, line?.speakerId)
        assertEquals("张三", line?.speakerName)
        assertEquals("今天天气不错。", line?.text)
    }

    @Test
    fun `parse narration line`() {
        val line = TownSceneParser.parseLine("【旁白】清晨的阳光洒在广场上。", nameToId)
        assertEquals("narration", line?.kind)
        assertNull(line?.speakerId)
        assertNull(line?.speakerName)
        assertEquals("清晨的阳光洒在广场上。", line?.text)
    }

    @Test
    fun `parse action and thought lines`() {
        val action = TownSceneParser.parseLine("【动作|李四】放下手中的酒杯", nameToId)
        assertEquals("action", action?.kind)
        assertEquals(2L, action?.speakerId)

        val thought = TownSceneParser.parseLine("【心理|张三】他今天怎么怪怪的。", nameToId)
        assertEquals("thought", thought?.kind)
        assertEquals(1L, thought?.speakerId)
    }

    @Test
    fun `supports half width brackets and separator`() {
        val line = TownSceneParser.parseLine("[对话|张三]你好。", nameToId)
        assertEquals("dialogue", line?.kind)
        assertEquals("你好。", line?.text)
    }

    @Test
    fun `unknown speaker has null id but keeps name`() {
        val line = TownSceneParser.parseLine("【对话|王五】我是谁？", nameToId)
        assertEquals("dialogue", line?.kind)
        assertNull(line?.speakerId)
        assertEquals("王五", line?.speakerName)
    }

    @Test
    fun `plain text without marker is rejected`() {
        assertNull(TownSceneParser.parseLine("这是一句没有标记的话。", nameToId))
        assertNull(TownSceneParser.parseLine("", nameToId))
    }

    @Test
    fun `parse multi-line text skips invalid lines`() {
        val text = """
            【旁白】夜幕降临。
            这行没有标记，应被跳过
            【对话|张三】走吧。
        """.trimIndent()
        val lines = TownSceneParser.parse(text, nameToId)
        assertEquals(2, lines.size)
        assertEquals("narration", lines[0].kind)
        assertEquals("dialogue", lines[1].kind)
    }
}
