package org.jiangstack.mytavern.domain.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import org.jiangstack.mytavern.data.remote.Tool
import org.jiangstack.mytavern.data.remote.ToolCall
import org.jiangstack.mytavern.data.remote.ToolFunction
import org.jiangstack.mytavern.data.remote.ToolParameters
import org.jiangstack.mytavern.data.remote.ToolProperty
import org.jiangstack.mytavern.domain.model.ChatMessage
import org.jiangstack.mytavern.domain.model.InteractiveGame
import org.jiangstack.mytavern.domain.model.InteractiveGameState
import org.jiangstack.mytavern.domain.model.InteractiveMessage
import org.jiangstack.mytavern.domain.model.PromptBlockConfig
import org.jiangstack.mytavern.domain.model.PromptBlockDefaults
import org.jiangstack.mytavern.domain.model.PromptBlockType
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.InteractiveGameRepository
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository

class InteractiveStoryService(
    private val llmService: LlmService,
    private val gameRepository: InteractiveGameRepository,
    private val characterRepository: CharacterRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val json: Json
) {
    sealed class StoryEvent {
        data class TextDelta(val content: String) : StoryEvent()
        data class ToolResult(val toolName: String, val result: String) : StoryEvent()
        data class ActionOptions(val options: List<String>) : StoryEvent()
        data class Error(val message: String) : StoryEvent()
        data class FinalResponse(val content: String) : StoryEvent()
    }

    companion object {
        const val MAX_ITERATIONS = 5

        val updateCharacterStatusTool = Tool(
            function = ToolFunction(
                name = "update_character_status",
                description = "更新人物当前状态，包括健康、情绪、体力等信息。",
                parameters = ToolParameters(
                    properties = mapOf(
                        "status" to ToolProperty("string", "人物状态描述")
                    ),
                    required = listOf("status")
                )
            )
        )

        val updateCharacterItemsTool = Tool(
            function = ToolFunction(
                name = "update_character_items",
                description = "更新人物当前持有的物品列表。",
                parameters = ToolParameters(
                    properties = mapOf(
                        "items" to ToolProperty("string", "人物物品列表描述")
                    ),
                    required = listOf("items")
                )
            )
        )

        val updateEnvironmentTool = Tool(
            function = ToolFunction(
                name = "update_environment",
                description = "更新当前故事发生的环境/地点描述。",
                parameters = ToolParameters(
                    properties = mapOf(
                        "environment" to ToolProperty("string", "当前环境描述")
                    ),
                    required = listOf("environment")
                )
            )
        )

        val provideActionOptionsTool = Tool(
            function = ToolFunction(
                name = "provide_action_options",
                description = "为用户提供3-5个行动选项供选择。",
                parameters = ToolParameters(
                    properties = mapOf(
                        "options" to ToolProperty("string", "JSON数组，包含3-5个行动选项字符串，例如：[\"选项1\",\"选项2\",\"选项3\"]")
                    ),
                    required = listOf("options")
                )
            )
        )

        val storyTools = listOf(
            updateCharacterStatusTool,
            updateCharacterItemsTool,
            updateEnvironmentTool,
            provideActionOptionsTool
        )
    }

    fun runStoryTurn(
        game: InteractiveGame,
        messages: List<InteractiveMessage>,
        gameState: InteractiveGameState?,
        userAction: String,
        freeMode: Boolean = false
    ): Flow<StoryEvent> = flow {
        val (systemPrompt, chatMessages) = buildMessages(game, messages, gameState, userAction)

        var iteration = 0
        val collectedActionOptions = mutableListOf<String>()

        while (iteration < MAX_ITERATIONS) {
            iteration++
            val fullContent = StringBuilder()
            var toolCalls: List<ToolCall>? = null
            var lastFinishReason: String? = null

            try {
                llmService.sendChatMessageStream(
                    messages = chatMessages,
                    systemPrompt = systemPrompt,
                    tools = storyTools,
                    skipMessagePrefix = true
                ).collect { chunk ->
                    if (chunk.content.isNotBlank()) {
                        fullContent.append(chunk.content)
                        emit(StoryEvent.TextDelta(chunk.content))
                    }
                    if (chunk.toolCalls != null) {
                        toolCalls = chunk.toolCalls
                    }
                    if (chunk.finishReason != null) {
                        lastFinishReason = chunk.finishReason
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is java.net.UnknownHostException -> "网络连接失败，请检查网络设置"
                    is java.net.SocketTimeoutException -> "网络请求超时，请稍后再试"
                    is java.io.IOException -> e.message ?: "网络请求失败"
                    else -> e.message ?: "请求失败"
                }
                emit(StoryEvent.Error(errorMsg))
                return@flow
            }

            if (lastFinishReason == "length") {
                emit(StoryEvent.Error("已达到最大输出长度限制，回复被截断。已输出的内容已保留。"))
            } else if (lastFinishReason == "content_filter") {
                emit(StoryEvent.Error("内容触发了安全过滤，回复被中断。已输出的内容已保留。"))
            }

            if (toolCalls.isNullOrEmpty()) {
                emit(StoryEvent.FinalResponse(fullContent.toString()))
                emit(StoryEvent.ActionOptions(collectedActionOptions))
                return@flow
            }

            chatMessages.add(ChatMessage(sessionId = 0, content = fullContent.toString(), role = "assistant"))

            for (tc in toolCalls!!) {
                val args = try {
                    json.decodeFromString<Map<String, JsonElement>>(tc.function.arguments)
                } catch (_: Exception) {
                    emptyMap()
                }

                val result = when (tc.function.name) {
                    "update_character_status" -> {
                        val status = args["status"]?.jsonPrimitive?.content ?: ""
                        gameRepository.insertOrUpdateGameState(
                            InteractiveGameState(
                                gameId = game.id,
                                environment = gameState?.environment ?: "",
                                characterStatus = status,
                                characterItems = gameState?.characterItems ?: ""
                            )
                        )
                        "人物状态已更新。"
                    }
                    "update_character_items" -> {
                        val items = args["items"]?.jsonPrimitive?.content ?: ""
                        gameRepository.insertOrUpdateGameState(
                            InteractiveGameState(
                                gameId = game.id,
                                environment = gameState?.environment ?: "",
                                characterStatus = gameState?.characterStatus ?: "",
                                characterItems = items
                            )
                        )
                        "人物物品已更新。"
                    }
                    "update_environment" -> {
                        val environment = args["environment"]?.jsonPrimitive?.content ?: ""
                        gameRepository.insertOrUpdateGameState(
                            InteractiveGameState(
                                gameId = game.id,
                                environment = environment,
                                characterStatus = gameState?.characterStatus ?: "",
                                characterItems = gameState?.characterItems ?: ""
                            )
                        )
                        "环境已更新。"
                    }
                    "provide_action_options" -> {
                        val optionsElement = args["options"]
                        val options = try {
                            when (optionsElement) {
                                is kotlinx.serialization.json.JsonArray -> {
                                    json.decodeFromString<List<String>>(optionsElement.toString())
                                }
                                else -> {
                                    val optionsStr = optionsElement?.jsonPrimitive?.content ?: "[]"
                                    json.decodeFromString<List<String>>(optionsStr)
                                }
                            }
                        } catch (_: Exception) {
                            emptyList()
                        }
                        collectedActionOptions.clear()
                        collectedActionOptions.addAll(options)
                        "行动选项已记录。"
                    }
                    else -> "未知工具: ${tc.function.name}"
                }

                emit(StoryEvent.ToolResult(tc.function.name, result))
                chatMessages.add(ChatMessage(sessionId = 0, content = result, role = "tool"))
            }
        }

        emit(StoryEvent.Error("已达到最大迭代轮次($MAX_ITERATIONS)，故事推进结束。"))
        emit(StoryEvent.ActionOptions(collectedActionOptions))
    }

    private fun PromptBlockType.messageRole(): String? = when (this) {
        PromptBlockType.INTERACTIVE_SYSTEM_ROLE,
        PromptBlockType.INTERACTIVE_NARRATOR_STYLE,
        PromptBlockType.INTERACTIVE_PARTICIPATING_CHARACTERS,
        PromptBlockType.INTERACTIVE_PLAY_CHARACTER,
        PromptBlockType.INTERACTIVE_STORY_BACKGROUND,
        PromptBlockType.INTERACTIVE_STORY_MAIN_PLOT -> "system"

        PromptBlockType.INTERACTIVE_STORY_CONTENT,
        PromptBlockType.INTERACTIVE_CURRENT_STATE -> "assistant"

        PromptBlockType.INTERACTIVE_USER_CHOICE,
        PromptBlockType.INTERACTIVE_OUTPUT_INSTRUCTION -> "user"

        else -> null
    }

    private suspend fun buildMessages(
        game: InteractiveGame,
        messages: List<InteractiveMessage>,
        gameState: InteractiveGameState?,
        userAction: String
    ): Pair<String, MutableList<ChatMessage>> {
        val blocks = userPreferencesRepository.interactivePromptBlocks
            .first()
            .filter { it.isEnabled }
            .sortedBy { it.sortOrder }

        val systemParts = mutableListOf<String>()
        val chatMessages = mutableListOf<ChatMessage>()

        for (block in blocks) {
            val role = block.type.messageRole() ?: continue
            val content = buildBlockContent(block, game, gameState, messages, userAction, game.windowWordCount)
            if (content.isNullOrBlank()) continue

            if (role == "system") {
                systemParts.add(content)
            } else {
                chatMessages.add(ChatMessage(sessionId = 0, content = content, role = role))
            }
        }

        val systemPrompt = systemParts.joinToString("\n\n")
        return systemPrompt to chatMessages
    }

    private suspend fun buildBlockContent(
        block: PromptBlockConfig,
        game: InteractiveGame,
        gameState: InteractiveGameState?,
        messages: List<InteractiveMessage>,
        userAction: String,
        windowWordCount: Int
    ): String? {
        return when (block.type) {
            PromptBlockType.INTERACTIVE_SYSTEM_ROLE,
            PromptBlockType.INTERACTIVE_OUTPUT_INSTRUCTION -> {
                block.customContent ?: PromptBlockDefaults.defaultContent(block.type)
            }

            PromptBlockType.INTERACTIVE_NARRATOR_STYLE -> {
                if (game.narratorStyle.isNotBlank()) "## 讲述者风格\n${game.narratorStyle}" else null
            }

            PromptBlockType.INTERACTIVE_PARTICIPATING_CHARACTERS -> {
                val characters = characterRepository.getCharactersByIds(game.characterIds)
                val names = characters.map { it.name }
                if (names.isNotEmpty()) {
                    buildString {
                        appendLine("## 参与角色")
                        names.forEach { appendLine("- $it") }
                    }.trimEnd()
                } else null
            }

            PromptBlockType.INTERACTIVE_PLAY_CHARACTER -> {
                val character = characterRepository.getCharactersByIds(listOf(game.playCharacterId)).firstOrNull()
                val name = character?.name ?: "未知角色"
                "## 用户扮演的角色\n$name"
            }

            PromptBlockType.INTERACTIVE_STORY_BACKGROUND -> {
                if (game.storyBackground.isNotBlank()) "## 故事背景\n${game.storyBackground}" else null
            }

            PromptBlockType.INTERACTIVE_STORY_MAIN_PLOT -> {
                if (game.storyMainPlot.isNotBlank()) "## 故事主线\n${game.storyMainPlot}" else null
            }

            PromptBlockType.INTERACTIVE_STORY_CONTENT -> {
                val narratorMessages = messages.filter { it.role == "narrator" }
                if (narratorMessages.isEmpty()) return null
                val fullContent = narratorMessages.joinToString("\n\n") { it.content }
                if (fullContent.length > windowWordCount) {
                    "..." + fullContent.takeLast(windowWordCount)
                } else {
                    fullContent
                }
            }

            PromptBlockType.INTERACTIVE_CURRENT_STATE -> {
                if (gameState == null) return null
                buildString {
                    if (gameState.environment.isNotBlank()) appendLine("当前环境：${gameState.environment}")
                    if (gameState.characterStatus.isNotBlank()) appendLine("人物状态：${gameState.characterStatus}")
                    if (gameState.characterItems.isNotBlank()) appendLine("人物物品：${gameState.characterItems}")
                }.trimEnd().takeIf { it.isNotBlank() }
            }

            PromptBlockType.INTERACTIVE_USER_CHOICE -> {
                if (userAction.isNotBlank()) "用户选择：$userAction" else null
            }

            else -> null
        }
    }
}
