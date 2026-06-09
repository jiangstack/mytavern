package org.jiangstack.mytavern.domain.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.jiangstack.mytavern.data.remote.Tool
import org.jiangstack.mytavern.data.remote.ToolCall
import org.jiangstack.mytavern.data.remote.ToolFunction
import org.jiangstack.mytavern.data.remote.ToolParameters
import org.jiangstack.mytavern.data.remote.ToolProperty
import org.jiangstack.mytavern.domain.model.ChatMessage
import org.jiangstack.mytavern.domain.model.Novel
import org.jiangstack.mytavern.domain.model.NovelChapter
import org.jiangstack.mytavern.domain.repository.NovelRepository

class NovelAgentService(
    private val llmService: LlmService,
    private val novelRepository: NovelRepository,
    private val json: Json
) {
    sealed class AgentEvent {
        data class Thinking(val content: String) : AgentEvent()
        data class TextDelta(val content: String) : AgentEvent()
        data class ToolCallStart(val toolName: String, val args: String) : AgentEvent()
        data class ToolResult(val toolName: String, val result: String) : AgentEvent()
        data class WriteApprovalNeeded(
            val chapterNumber: Int,
            val title: String,
            val content: String,
            val deferred: CompletableDeferred<Boolean>
        ) : AgentEvent()
        data class FinalResponse(val content: String) : AgentEvent()
        data class Error(val message: String) : AgentEvent()
    }

    companion object {
        const val MAX_ITERATIONS = 10

        val readOutlineTool = Tool(
            function = ToolFunction(
                name = "read_novel_outline",
                description = "读取当前小说指定范围的章节大纲。返回指定章节范围内的大纲信息，包括章节号、标题和大纲内容。",
                parameters = ToolParameters(
                    properties = mapOf(
                        "start_chapter" to ToolProperty("integer", "起始章节号（从1开始）"),
                        "end_chapter" to ToolProperty("integer", "结束章节号（包含此章节）")
                    ),
                    required = listOf("start_chapter", "end_chapter")
                )
            )
        )

        val readChapterTool = Tool(
            function = ToolFunction(
                name = "read_novel_chapter",
                description = "读取当前小说指定章节的完整内容。",
                parameters = ToolParameters(
                    properties = mapOf(
                        "chapter_number" to ToolProperty("integer", "章节号（从1开始）")
                    ),
                    required = listOf("chapter_number")
                )
            )
        )

        val writeChapterTool = Tool(
            function = ToolFunction(
                name = "write_novel_chapter",
                description = "写入或更新当前小说指定章节的内容。此操作需要用户批准后才会执行。可以用来创建新章节或修改已有章节。",
                parameters = ToolParameters(
                    properties = mapOf(
                        "chapter_number" to ToolProperty("integer", "章节号（从1开始）"),
                        "title" to ToolProperty("string", "章节标题"),
                        "outline" to ToolProperty("string", "章节大纲/摘要"),
                        "content" to ToolProperty("string", "章节正文内容")
                    ),
                    required = listOf("chapter_number", "title", "content")
                )
            )
        )

        val agentTools = listOf(readOutlineTool, readChapterTool, writeChapterTool)
    }

    fun runAgentLoop(
        novelId: Long,
        messages: List<ChatMessage>,
        systemPrompt: String,
        thinkingEnabled: Boolean = true,
        temperature: Float? = null,
        maxTokens: Int? = null
    ): Flow<AgentEvent> = flow {
        val conversation = messages.toMutableList()
        var iteration = 0

        while (iteration < MAX_ITERATIONS) {
            iteration++
            val fullContent = StringBuilder()
            var toolCalls: List<ToolCall>? = null
            var finalUsage: org.jiangstack.mytavern.data.remote.Usage? = null

            llmService.sendChatMessageStream(
                messages = conversation,
                systemPrompt = systemPrompt,
                temperature = temperature,
                maxTokens = maxTokens,
                tools = agentTools,
                thinkingEnabled = thinkingEnabled,
                skipMessagePrefix = true
            ).collect { chunk ->
                if (chunk.reasoningContent.isNotBlank()) {
                    emit(AgentEvent.Thinking(chunk.reasoningContent))
                }
                if (chunk.content.isNotBlank()) {
                    fullContent.append(chunk.content)
                    emit(AgentEvent.TextDelta(chunk.content))
                }
                if (chunk.toolCalls != null) {
                    toolCalls = chunk.toolCalls
                }
                if (chunk.usage != null) {
                    finalUsage = chunk.usage
                }
            }

            finalUsage?.let { UsageStatsTracker.recordUsage(it) }

            if (toolCalls.isNullOrEmpty()) {
                emit(AgentEvent.FinalResponse(fullContent.toString()))
                return@flow
            }

            conversation.add(ChatMessage(
                sessionId = 0,
                content = fullContent.toString(),
                role = "assistant"
            ))

            for (tc in toolCalls!!) {
                val args = try {
                    json.decodeFromString<Map<String, JsonElement>>(tc.function.arguments)
                } catch (e: Exception) {
                    emptyMap()
                }
                emit(AgentEvent.ToolCallStart(tc.function.name, tc.function.arguments))

                val result = when (tc.function.name) {
                    "read_novel_outline" -> {
                        val start = args["start_chapter"]?.jsonPrimitive?.int ?: 1
                        val end = args["end_chapter"]?.jsonPrimitive?.int ?: start
                        executeReadOutline(novelId, start, end)
                    }
                    "read_novel_chapter" -> {
                        val num = args["chapter_number"]?.jsonPrimitive?.int ?: 1
                        executeReadChapter(novelId, num)
                    }
                    "write_novel_chapter" -> {
                        val num = args["chapter_number"]?.jsonPrimitive?.int ?: 0
                        val title = args["title"]?.jsonPrimitive?.content ?: ""
                        val content = args["content"]?.jsonPrimitive?.content ?: ""
                        val outline = args["outline"]?.jsonPrimitive?.content ?: ""
                        val deferred = CompletableDeferred<Boolean>()
                        emit(AgentEvent.WriteApprovalNeeded(num, title, content, deferred))
                        if (deferred.await()) {
                            executeWriteChapter(novelId, num, title, outline, content)
                            "写入成功：第${num}章「${title}」已保存。"
                        } else {
                            "用户拒绝了本次写入操作。"
                        }
                    }
                    else -> "未知工具: ${tc.function.name}"
                }

                emit(AgentEvent.ToolResult(tc.function.name, result))
                conversation.add(ChatMessage(
                    sessionId = 0,
                    content = result,
                    role = "tool"
                ))
            }
        }

        emit(AgentEvent.Error("已达到最大迭代轮次($MAX_ITERATIONS)，智能体循环结束。"))
    }

    fun buildDefaultPrompt(novel: Novel, chapters: List<NovelChapter>): String {
        return buildString {
            appendLine("你是一个专业的小说创作助手。你可以帮助用户规划、撰写和修改小说内容。")
            appendLine()
            appendLine("## 当前小说信息")
            appendLine("- 标题：${novel.title}")
            appendLine("- 描述：${novel.description}")
            appendLine("- 已有章节数：${chapters.size}")
            if (chapters.isNotEmpty()) {
                appendLine("- 章节列表：")
                chapters.forEach { ch ->
                    appendLine("  第${ch.chapterNumber}章：${ch.title}")
                }
            }
            appendLine()
            appendLine("## 你可以使用的工具")
            appendLine("1. read_novel_outline：读取指定范围的章节大纲")
            appendLine("2. read_novel_chapter：读取指定章节的完整内容")
            appendLine("3. write_novel_chapter：写入或更新章节内容（需要用户批准）")
            appendLine()
            appendLine("## 工作原则")
            appendLine("- 在修改章节内容前，先通过工具读取现有内容了解上下文")
            appendLine("- 写入内容前向用户确认你的修改计划")
            appendLine("- 保持小说的风格和设定一致性")
            appendLine("- 如果用户没有指定具体操作，主动询问用户需要什么帮助")
        }
    }

    private suspend fun executeReadOutline(novelId: Long, start: Int, end: Int): String {
        val chapters = novelRepository.getChaptersByNovelIdSync(novelId)
        val filtered = chapters.filter { it.chapterNumber in start..end }
        if (filtered.isEmpty()) return "未找到第${start}到第${end}章的章节。"
        return buildString {
            appendLine("第${start}到第${end}章的大纲：")
            filtered.forEach { ch ->
                appendLine()
                appendLine("第${ch.chapterNumber}章：${ch.title}")
                if (ch.outline.isNotBlank()) {
                    appendLine("大纲：${ch.outline}")
                } else {
                    appendLine("大纲：（暂无大纲）")
                }
            }
        }
    }

    private suspend fun executeReadChapter(novelId: Long, chapterNumber: Int): String {
        val chapters = novelRepository.getChaptersByNovelIdSync(novelId)
        val chapter = chapters.find { it.chapterNumber == chapterNumber }
            ?: return "未找到第${chapterNumber}章。当前小说共有${chapters.size}章。"
        return buildString {
            appendLine("第${chapter.chapterNumber}章：${chapter.title}")
            appendLine()
            if (chapter.outline.isNotBlank()) {
                appendLine("【大纲】")
                appendLine(chapter.outline)
                appendLine()
            }
            if (chapter.content.isNotBlank()) {
                appendLine("【正文】")
                appendLine(chapter.content)
            } else {
                appendLine("（暂无正文内容）")
            }
        }
    }

    private suspend fun executeWriteChapter(
        novelId: Long,
        chapterNumber: Int,
        title: String,
        outline: String,
        content: String
    ): String {
        val chapters = novelRepository.getChaptersByNovelIdSync(novelId)
        val existing = chapters.find { it.chapterNumber == chapterNumber }
        if (existing != null) {
            novelRepository.updateChapter(existing.copy(
                title = title,
                outline = outline.ifBlank { existing.outline },
                content = content
            ))
        } else {
            novelRepository.insertChapter(NovelChapter(
                novelId = novelId,
                chapterNumber = chapterNumber,
                title = title,
                outline = outline,
                content = content
            ))
        }
        return "ok"
    }
}
