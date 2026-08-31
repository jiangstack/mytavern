package org.jiangstack.mytavern.domain.service

import org.jiangstack.mytavern.domain.model.SceneLine

/**
 * AVG 场景文本行解析。
 * 行格式：【旁白】xxx / 【对话|角色名】xxx / 【动作|角色名】xxx / 【心理|角色名】xxx
 * 兼容半角方括号 []、全角/半角分隔符 | 与 ｜。
 */
object TownSceneParser {
    private val lineRegex = Regex("""^[ \t]*[【\[](旁白|对话|动作|心理)(?:[|｜]([^】\]]+))?[】\]][ \t]*(.*)$""")

    fun parseLine(raw: String, nameToId: Map<String, Long>): SceneLine? {
        val match = lineRegex.find(raw.trim()) ?: return null
        val kind = when (match.groupValues[1]) {
            "旁白" -> "narration"
            "对话" -> "dialogue"
            "动作" -> "action"
            else -> "thought"
        }
        val name = match.groupValues[2].trim().takeIf { it.isNotEmpty() }
        val text = match.groupValues[3].trim()
        if (text.isEmpty()) return null
        return SceneLine(
            kind = kind,
            speakerId = name?.let { nameToId[it] },
            speakerName = name,
            text = text
        )
    }

    fun parse(text: String, nameToId: Map<String, Long>): List<SceneLine> =
        text.lines().mapNotNull { parseLine(it, nameToId) }

    /** 反序列化为模型提示词里使用的行格式（用于把已有剧情回传给 LLM）。 */
    fun toLineText(line: SceneLine): String = when (line.kind) {
        "narration" -> "【旁白】${line.text}"
        "dialogue" -> "【对话|${line.speakerName ?: "未知"}】${line.text}"
        "action" -> "【动作|${line.speakerName ?: "未知"}】${line.text}"
        else -> "【心理|${line.speakerName ?: "未知"}】${line.text}"
    }
}
