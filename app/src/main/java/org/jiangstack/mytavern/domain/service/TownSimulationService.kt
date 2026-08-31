package org.jiangstack.mytavern.domain.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import org.jiangstack.mytavern.data.remote.Tool
import org.jiangstack.mytavern.data.remote.ToolCall
import org.jiangstack.mytavern.data.remote.ToolFunction
import org.jiangstack.mytavern.data.remote.ToolParameters
import org.jiangstack.mytavern.data.remote.ToolProperty
import org.jiangstack.mytavern.domain.model.ChatMessage
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.EventPlan
import org.jiangstack.mytavern.domain.model.LogKind
import org.jiangstack.mytavern.domain.model.SceneLine
import org.jiangstack.mytavern.domain.model.SceneStatus
import org.jiangstack.mytavern.domain.model.SceneType
import org.jiangstack.mytavern.domain.model.ScheduleChange
import org.jiangstack.mytavern.domain.model.ScheduleItem
import org.jiangstack.mytavern.domain.model.Town
import org.jiangstack.mytavern.domain.model.TownLocation
import org.jiangstack.mytavern.domain.model.TownLogEntry
import org.jiangstack.mytavern.domain.model.TownMember
import org.jiangstack.mytavern.domain.model.TownMemoryEntry
import org.jiangstack.mytavern.domain.model.TownRelationship
import org.jiangstack.mytavern.domain.model.TownScene
import org.jiangstack.mytavern.domain.model.TownSnapshot
import org.jiangstack.mytavern.domain.model.TownSnapshotData
import org.jiangstack.mytavern.domain.model.PromptBlockDefaults
import org.jiangstack.mytavern.domain.model.PromptBlockType
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.TownRepository
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository

/**
 * 小镇模拟核心服务。
 *
 * 核心原则：tick 与 LLM 解耦。逐小时推进只做确定性的日程套用（零 LLM 成本），
 * LLM 仅在跨天行动表生成、相遇/事件场景生成、玩家互动、世界事件展开时调用。
 */
class TownSimulationService(
    private val llmService: LlmService,
    private val townRepository: TownRepository,
    private val characterRepository: CharacterRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val json: Json
) {

    /** 应用级作用域：场景生成在其中的协程执行，退出页面（ViewModel 销毁）不中断 */
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    sealed class SceneEvent {
        /** 当前行流式增量（打字机） */
        data class LineDelta(val text: String) : SceneEvent()

        /** 一行台词完成 */
        data class LineAppended(val line: SceneLine) : SceneEvent()

        data class ToolResult(val toolName: String, val result: String) : SceneEvent()
        data class Finished(val scene: TownScene) : SceneEvent()
        data class Error(val message: String) : SceneEvent()
    }

    sealed class AdvanceResult {
        data class Quiet(val hoursAdvanced: Int) : AdvanceResult()
        data class SceneCreated(val sceneId: Long) : AdvanceResult()
        data class InteractionPending(val sceneId: Long) : AdvanceResult()
        data class Failed(val message: String) : AdvanceResult()
    }

    companion object {
        private const val NIGHT_START_HOUR = 0
        private const val NIGHT_END_HOUR = 6
        private const val ENCOUNTER_DEDUPE_HOURS = 2
        private const val MAX_RECENT_MEMORY = 10
        private const val MAX_IMPORTANT_MEMORY = 10
        private const val CONTEXT_LOG_COUNT = 30
        private const val SUMMARY_MAX_LENGTH = 80

        private val setMemberScheduleTool = Tool(
            function = ToolFunction(
                name = "set_member_schedule",
                description = "设置某个居民的当日行动表。行动表必须覆盖0-24小时，时段按[startHour,endHour)表示。",
                parameters = ToolParameters(
                    properties = mapOf(
                        "memberId" to ToolProperty("string", "居民ID"),
                        "scheduleJson" to ToolProperty(
                            "string",
                            "行动表JSON数组，如：[{\"startHour\":8,\"endHour\":12,\"locationId\":1,\"activity\":\"在广场上摆摊\"}]"
                        )
                    ),
                    required = listOf("memberId", "scheduleJson")
                )
            )
        )

        private val updateMoodTool = Tool(
            function = ToolFunction(
                name = "update_mood",
                description = "更新某个角色当前的心情（简短短语，如：愉悦、烦躁、紧张）。",
                parameters = ToolParameters(
                    properties = mapOf(
                        "memberId" to ToolProperty("string", "角色ID"),
                        "mood" to ToolProperty("string", "心情描述")
                    ),
                    required = listOf("memberId", "mood")
                )
            )
        )

        private val updateRelationshipTool = Tool(
            function = ToolFunction(
                name = "update_relationship",
                description = "更新两个角色之间的关系：好感度变化(-100到100的整数增量)与一句话关系描述。",
                parameters = ToolParameters(
                    properties = mapOf(
                        "memberAId" to ToolProperty("string", "角色A的ID"),
                        "memberBId" to ToolProperty("string", "角色B的ID"),
                        "affinityDelta" to ToolProperty("string", "好感度变化量，整数，如 5 或 -10"),
                        "note" to ToolProperty("string", "一句话描述当前关系，如：因争吵而疏远")
                    ),
                    required = listOf("memberAId", "memberBId", "affinityDelta", "note")
                )
            )
        )

        private val addImportantMemoryTool = Tool(
            function = ToolFunction(
                name = "add_important_memory",
                description = "为本场景中某个角色记录一条重要的长期记忆（仅在该角色经历了重要事件时调用）。",
                parameters = ToolParameters(
                    properties = mapOf(
                        "memberId" to ToolProperty("string", "角色ID"),
                        "content" to ToolProperty("string", "重要记忆内容（一句话）")
                    ),
                    required = listOf("memberId", "content")
                )
            )
        )

        private val finishSceneTool = Tool(
            function = ToolFunction(
                name = "finish_scene",
                description = "结束场景并给出一句话总结（作为本场景的结果描述）。",
                parameters = ToolParameters(
                    properties = mapOf(
                        "summary" to ToolProperty("string", "一句话总结本场景发生了什么")
                    ),
                    required = listOf("summary")
                )
            )
        )

        private val sceneStateTools = listOf(updateMoodTool, updateRelationshipTool, addImportantMemoryTool)

        private val createEventPlanTool = Tool(
            function = ToolFunction(
                name = "create_event_plan",
                description = "根据用户的简短描述，制定一个完整的世界事件方案。",
                parameters = ToolParameters(
                    properties = mapOf(
                        "title" to ToolProperty("string", "事件标题"),
                        "description" to ToolProperty("string", "事件的完整描述：起因、经过、结果走向"),
                        "locationId" to ToolProperty("string", "事件发生地点的ID"),
                        "participantIdsJson" to ToolProperty("string", "参与角色的ID列表JSON数组，如：[1,3]"),
                        "scheduleChangesJson" to ToolProperty(
                            "string",
                            "对当日行动表的修改，JSON数组，如：[{\"memberId\":1,\"items\":[{\"startHour\":10,\"endHour\":12,\"locationId\":2,\"activity\":\"围观事件\"}]}]，无修改则为[]"
                        )
                    ),
                    required = listOf("title", "description", "locationId", "participantIdsJson", "scheduleChangesJson")
                )
            )
        )
    }

    // ========== 行动表生成 ==========

    /**
     * 生成（或强制重新生成）当日全员行动表。小镇创建后与每日跨天时调用。
     */
    suspend fun ensureSchedules(townId: Long, force: Boolean = false): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val town = townRepository.getTownById(townId)
                    ?: return@withContext Result.failure(IllegalStateException("小镇不存在"))
                val npcs = townRepository.getMembersByTownIdSync(townId).filter { !it.isPlayerControlled }
                if (npcs.isEmpty()) return@withContext Result.success(Unit)
                if (!force && npcs.all { it.todaySchedule.isNotEmpty() }) {
                    return@withContext Result.success(Unit)
                }
                val locations = townRepository.getLocationsByTownIdSync(townId)
                if (locations.isEmpty()) {
                    return@withContext Result.failure(IllegalStateException("请先为小镇配置地点"))
                }

                val characters = characterRepository.getCharactersByIds(npcs.map { it.characterId })
                val nameByMemberId = buildMemberNameMap(npcs, characters)
                val relationships = townRepository.getRelationshipsByTownIdSync(townId)

                val systemPrompt = buildScheduleSystemPrompt(town, npcs, nameByMemberId, locations, relationships)
                val chatMessages = mutableListOf(
                    ChatMessage(sessionId = 0, content = "请生成第${town.currentDay}天所有居民的行动表。", role = "user")
                )

                val maxIterations = userPreferencesRepository.townMaxIterations.first()
                var iteration = 0
                while (iteration < maxIterations) {
                    iteration++
                    val contentBuilder = StringBuilder()
                    var toolCalls: List<ToolCall>? = null
                    llmService.sendChatMessageStream(
                        messages = chatMessages,
                        systemPrompt = systemPrompt,
                        tools = listOf(setMemberScheduleTool),
                        skipMessagePrefix = true
                    ).collect { chunk ->
                        if (chunk.content.isNotBlank()) contentBuilder.append(chunk.content)
                        if (chunk.toolCalls != null) toolCalls = chunk.toolCalls
                    }

                    if (toolCalls.isNullOrEmpty()) break
                    chatMessages.add(ChatMessage(sessionId = 0, content = contentBuilder.toString(), role = "assistant", toolCalls = toolCalls))

                    for (tc in toolCalls!!) {
                        val args = parseToolArgs(tc.function.arguments)
                        val result = when (tc.function.name) {
                            "set_member_schedule" -> {
                                val memberId = args["memberId"]?.jsonPrimitive?.content?.toLongOrNull()
                                val scheduleJson = args["scheduleJson"]?.jsonPrimitive?.content ?: "[]"
                                val member = npcs.firstOrNull { it.id == memberId }
                                if (member == null) {
                                    "未知成员ID: $memberId"
                                } else {
                                    val items = TownScheduleValidator.sanitize(
                                        scheduleJson,
                                        locations.map { it.id }.toSet(),
                                        locations.first().id
                                    )
                                    townRepository.updateMember(member.copy(todaySchedule = items))
                                    "已保存 ${nameByMemberId[member.id] ?: memberId} 的行动表（${items.size}个时段）。"
                                }
                            }
                            else -> "未知工具: ${tc.function.name}"
                        }
                        chatMessages.add(ChatMessage(sessionId = 0, content = result, role = "tool", toolCallId = tc.id))
                    }
                }
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ========== 时间推进 ==========

    /**
     * 推进一小时。若队列中存在待生成场景则先生成场景而不推进时间。
     */
    suspend fun advanceOneHour(townId: Long): AdvanceResult = withContext(Dispatchers.IO) {
        try {
            val town = townRepository.getTownById(townId)
                ?: return@withContext AdvanceResult.Failed("小镇不存在")
            townRepository.getActiveInteractiveScene(townId)?.let {
                return@withContext AdvanceResult.Failed("有进行中的互动场景，请先结束它")
            }

            // 队列中还有待生成场景：直接交给场景页生成（可流式观看），推进在此停下
            townRepository.getFirstPendingScene(townId)?.let { pending ->
                return@withContext AdvanceResult.SceneCreated(pending.id)
            }

            // 1. 推进时间
            var newDay = town.currentDay
            var newHour = town.currentHour + 1
            if (newHour >= 24) {
                newHour = 0
                newDay += 1
            }
            townRepository.updateTown(town.copy(currentDay = newDay, currentHour = newHour))

            // 2. 跨天：生成新一天的行动表
            if (newHour == 0) {
                ensureSchedules(townId, force = true).onFailure { e ->
                    townRepository.insertLog(
                        TownLogEntry(
                            townId = townId, day = newDay, hour = newHour,
                            kind = LogKind.SYSTEM, text = "第${newDay}天行动表生成失败：${e.message}"
                        )
                    )
                }
            }

            // 3. 套用行动表（确定性，零 LLM 成本）
            val members = townRepository.getMembersByTownIdSync(townId)
            val locations = townRepository.getLocationsByTownIdSync(townId)
            val locationNameById = locations.associate { it.id to it.name }
            val characters = characterRepository.getCharactersByIds(members.map { it.characterId })
            val nameByMemberId = buildMemberNameMap(members, characters)
            val playerMember = town.playMemberId?.let { pid -> members.firstOrNull { it.id == pid } }

            for (member in members.filter { !it.isPlayerControlled }) {
                val slot = member.todaySchedule.firstOrNull { newHour >= it.startHour && newHour < it.endHour }
                townRepository.updateMember(
                    member.copy(
                        currentLocationId = slot?.locationId
                            ?: member.currentLocationId
                            ?: locations.firstOrNull()?.id,
                        currentActivity = slot?.activity ?: "自由活动"
                    )
                )
            }

            // 4. tick 日志
            val refreshed = townRepository.getMembersByTownIdSync(townId)
            val logText = refreshed.groupBy { it.currentLocationId }.entries.joinToString("；") { (locId, ms) ->
                val locName = locationNameById[locId] ?: "未知地点"
                val who = ms.joinToString("、") { nameByMemberId[it.id] ?: "?" }
                "$locName：$who"
            }
            townRepository.insertLog(
                TownLogEntry(townId = townId, day = newDay, hour = newHour, kind = LogKind.TICK, text = logText)
            )

            // 5. 玩家互动检查：有 NPC 与玩家同地点 → 挂起等待玩家互动
            if (playerMember != null) {
                val coLocatedNpcs = refreshed.filter {
                    !it.isPlayerControlled && it.currentLocationId == playerMember.currentLocationId
                }
                if (coLocatedNpcs.isNotEmpty()) {
                    val scene = TownScene(
                        townId = townId,
                        day = newDay,
                        hour = newHour,
                        locationId = playerMember.currentLocationId,
                        type = SceneType.INTERACTION,
                        status = SceneStatus.AWAITING_PLAYER,
                        participantIds = listOf(playerMember.id) + coLocatedNpcs.map { it.id }
                    )
                    val sceneId = townRepository.insertScene(scene)
                    townRepository.insertLog(
                        TownLogEntry(
                            townId = townId, day = newDay, hour = newHour, kind = LogKind.SYSTEM,
                            text = "${nameByMemberId[playerMember.id]}在${locationNameById[playerMember.currentLocationId] ?: "小镇"}与${coLocatedNpcs.map { nameByMemberId[it.id] }.joinToString("、")}相遇"
                        )
                    )
                    return@withContext AdvanceResult.InteractionPending(sceneId)
                }
            }

            // 6. 遭遇检测（夜间不生成；同组2小时内不重复生成）
            if (newHour !in NIGHT_START_HOUR until NIGHT_END_HOUR) {
                val groups = refreshed.filter { !it.isPlayerControlled }.groupBy { it.currentLocationId }
                for ((locId, group) in groups) {
                    if (locId == null || group.size < 2) continue
                    if (hasRecentEncounter(townId, locId, group.map { it.id }, newDay, newHour)) continue
                    val scene = TownScene(
                        townId = townId,
                        day = newDay,
                        hour = newHour,
                        locationId = locId,
                        type = SceneType.ENCOUNTER,
                        status = SceneStatus.PENDING,
                        participantIds = group.map { it.id }
                    )
                    val sceneId = townRepository.insertScene(scene)
                    // 不在此处生成：跳转场景页后再生成，用户可流式观看
                    return@withContext AdvanceResult.SceneCreated(sceneId)
                }
            }

            AdvanceResult.Quiet(1)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AdvanceResult.Failed(e.message ?: "推进失败")
        }
    }

    /**
     * 静默推进直到下一个事件（场景生成/玩家互动），最多推进24小时。
     */
    suspend fun advanceToNextEvent(
        townId: Long,
        onProgress: suspend (Int) -> Unit = {}
    ): AdvanceResult {
        var total = 0
        while (total < 24) {
            when (val result = advanceOneHour(townId)) {
                is AdvanceResult.Quiet -> {
                    total += result.hoursAdvanced
                    onProgress(total)
                }
                else -> return result
            }
        }
        return AdvanceResult.Quiet(total)
    }

    // ========== 场景生成与互动 ==========

    /**
     * 生成/推进一个场景（相遇、事件、互动开场、玩家输入后的续演）。
     */
    fun generateScene(
        sceneId: Long,
        finalStatus: SceneStatus = SceneStatus.DONE,
        recordMemory: Boolean = true,
        playerInput: String? = null,
        useFinishTool: Boolean = true
    ): Flow<SceneEvent> = flow {
        try {
            val scene = townRepository.getSceneById(sceneId)
            if (scene == null) {
                emit(SceneEvent.Error("场景不存在"))
                return@flow
            }
            val town = townRepository.getTownById(scene.townId)
            if (town == null) {
                emit(SceneEvent.Error("小镇不存在"))
                return@flow
            }
            val locations = townRepository.getLocationsByTownIdSync(scene.townId)
            val location = locations.firstOrNull { it.id == scene.locationId }
            val members = townRepository.getMembersByTownIdSync(scene.townId)
            val participants = members.filter { it.id in scene.participantIds }
            if (participants.isEmpty()) {
                emit(SceneEvent.Error("场景没有参与角色"))
                return@flow
            }
            val characters = characterRepository.getCharactersByIds(participants.map { it.characterId })
            val nameByMemberId = buildMemberNameMap(participants, characters)
            val idByName = nameByMemberId.entries.associate { (k, v) -> v to k }
            val playerMember = town.playMemberId?.let { pid -> participants.firstOrNull { it.id == pid } }

            townRepository.updateScene(scene.copy(status = SceneStatus.GENERATING))

            val (systemPrompt, chatMessages) = buildSceneMessages(
                town, location, participants, nameByMemberId, playerMember
            )

            if (scene.lines.isEmpty()) {
                chatMessages.add(ChatMessage(sessionId = 0, content = buildSceneTask(scene, location, nameByMemberId, playerMember), role = "user"))
            } else {
                chatMessages.add(
                    ChatMessage(
                        sessionId = 0,
                        content = "以下是本场景已有的剧情：\n" + scene.lines.joinToString("\n") { TownSceneParser.toLineText(it) },
                        role = "assistant"
                    )
                )
                chatMessages.add(
                    ChatMessage(
                        sessionId = 0,
                        content = "玩家扮演的${nameByMemberId[playerMember?.id] ?: "角色"}的行动：${playerInput ?: ""}\n请继续演绎其他角色的反应与剧情走向，保持格式不变。",
                        role = "user"
                    )
                )
            }

            val parser = LineStreamParser(idByName)

            // 已完成台词逐行落库：中途退出、进程被杀或强制结束时，已生成部分不丢失。
            // 写入前校验状态仍为生成中，避免与强制结束的完结写入互相覆盖。
            suspend fun persistPartialLines() {
                val current = townRepository.getSceneById(sceneId) ?: return
                if (current.status != SceneStatus.GENERATING) return
                townRepository.updateScene(current.copy(lines = parser.lines.toList()))
            }

            val maxIterations = userPreferencesRepository.townMaxIterations.first()
            var iteration = 0
            while (iteration < maxIterations) {
                iteration++
                val contentBuilder = StringBuilder()
                var toolCalls: List<ToolCall>? = null
                llmService.sendChatMessageStream(
                    messages = chatMessages,
                    systemPrompt = systemPrompt,
                    tools = sceneStateTools + if (useFinishTool) listOf(finishSceneTool) else emptyList(),
                    skipMessagePrefix = true
                ).collect { chunk ->
                    if (chunk.content.isNotBlank()) {
                        contentBuilder.append(chunk.content)
                        val newLines = parser.feed(chunk.content)
                        if (newLines.isNotEmpty()) {
                            newLines.forEach { emit(SceneEvent.LineAppended(it)) }
                            persistPartialLines()
                        }
                        val partial = parser.currentPartial()
                        if (partial.isNotEmpty()) emit(SceneEvent.LineDelta(partial))
                    }
                    if (chunk.toolCalls != null) toolCalls = chunk.toolCalls
                }
                val finishedLines = parser.finish()
                if (finishedLines.isNotEmpty()) {
                    finishedLines.forEach { emit(SceneEvent.LineAppended(it)) }
                    persistPartialLines()
                }

                if (toolCalls.isNullOrEmpty()) break
                chatMessages.add(ChatMessage(sessionId = 0, content = contentBuilder.toString(), role = "assistant", toolCalls = toolCalls))
                for (tc in toolCalls!!) {
                    val result = applySceneStateTool(tc, members, scene.townId, scene.day, scene.hour)
                    emit(SceneEvent.ToolResult(tc.function.name, result))
                    chatMessages.add(ChatMessage(sessionId = 0, content = result, role = "tool", toolCallId = tc.id))
                }
            }

            val lines = parser.lines
            if (lines.isEmpty()) {
                townRepository.updateScene(scene.copy(status = SceneStatus.PENDING))
                emit(SceneEvent.Error("场景生成结果为空，请重试"))
                return@flow
            }

            val summary = buildSceneSummary(lines)
            if (recordMemory) {
                appendSceneMemory(participants, scene, location, summary)
                townRepository.insertLog(
                    TownLogEntry(
                        townId = scene.townId,
                        day = scene.day,
                        hour = scene.hour,
                        kind = if (scene.type == SceneType.EVENT) LogKind.EVENT else LogKind.TICK,
                        text = "场景：$summary"
                    )
                )
            }
            val finished = scene.copy(status = finalStatus, lines = lines, summary = summary)
            townRepository.updateScene(finished)
            emit(SceneEvent.Finished(finished))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 生成失败时回退场景状态，允许重试
            try {
                townRepository.getSceneById(sceneId)?.let { current ->
                    if (current.status == SceneStatus.GENERATING) {
                        townRepository.updateScene(current.copy(status = SceneStatus.PENDING))
                    }
                }
            } catch (_: Exception) {
            }
            emit(SceneEvent.Error(e.message ?: "场景生成失败"))
        }
    }

    /**
     * 玩家离开/结束互动场景：提取状态变更（心情/关系/重要记忆），写入记忆并完结场景。
     */
    suspend fun endScene(sceneId: Long): Result<TownScene> = withContext(Dispatchers.IO) {
        try {
            endSceneCore(sceneId, forceStop = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 强制结束正在生成的场景：取消生成协程，保留已逐行落库的台词，立即收尾完结。
     * 没有台词的场景直接回退为 PENDING 等待下次补生成。
     */
    suspend fun forceStopScene(sceneId: Long): Result<TownScene> = withContext(Dispatchers.IO) {
        try {
            runningSceneJobs.remove(sceneId)?.cancel()
            endSceneCore(sceneId, forceStop = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun endSceneCore(sceneId: Long, forceStop: Boolean): Result<TownScene> {
        val scene = townRepository.getSceneById(sceneId)
            ?: return Result.failure(IllegalStateException("场景不存在"))
        if (scene.status == SceneStatus.DONE) return Result.success(scene)
        // 生成中：若有台词可强停完结，否则回退为 PENDING
        if (scene.status == SceneStatus.GENERATING) {
            if (!forceStop) return Result.failure(IllegalStateException("场景正在生成中，请稍后再试"))
            if (scene.lines.isEmpty()) {
                townRepository.updateScene(scene.copy(status = SceneStatus.PENDING))
                return Result.failure(IllegalStateException("还没有生成任何台词，已回退等待下次补生成"))
            }
        }
        if (scene.status != SceneStatus.INTERACTIVE && scene.status != SceneStatus.AWAITING_PLAYER &&
            scene.status != SceneStatus.GENERATING) {
            return Result.failure(IllegalStateException("场景已结束"))
        }
        val locations = townRepository.getLocationsByTownIdSync(scene.townId)
        val location = locations.firstOrNull { it.id == scene.locationId }
        val members = townRepository.getMembersByTownIdSync(scene.townId)
        val participants = members.filter { it.id in scene.participantIds }
        val characters = characterRepository.getCharactersByIds(participants.map { it.characterId })
        val nameByMemberId = buildMemberNameMap(participants, characters)

        val systemPrompt = "你是小镇生活模拟的状态记录助手。请根据以下场景内容，" +
            "更新各角色的心情、角色间的关系，并为经历重要事件的角色记录长期记忆，最后调用 finish_scene 给出一句话总结。"
        val chatMessages = mutableListOf<ChatMessage>(
            ChatMessage(
                sessionId = 0,
                content = buildString {
                    appendLine("时间：第${scene.day}天 ${scene.hour}:00")
                    appendLine("地点：${location?.name ?: "小镇"}")
                    appendLine("参与角色：${participants.mapNotNull { nameByMemberId[it.id] }.joinToString("、")}")
                    appendLine("角色近期记忆与关系：")
                    participants.forEach { m ->
                        appendLine("- ${nameByMemberId[m.id]}：心情${m.mood.ifBlank { "平静" }}；近期记忆：${m.recentMemory.takeLast(3).joinToString("；") { it.content }}")
                    }
                    appendLine()
                    appendLine("场景内容：")
                    scene.lines.forEach { appendLine(TownSceneParser.toLineText(it)) }
                },
                role = "user"
            ),
            ChatMessage(sessionId = 0, content = "场景到此结束。请调用工具完成状态更新。", role = "user")
        )

        val tools = sceneStateTools + finishSceneTool
        var summary = buildSceneSummary(scene.lines)
        val maxIterations = userPreferencesRepository.townMaxIterations.first()
        var iteration = 0
        while (iteration < maxIterations) {
            iteration++
            val contentBuilder = StringBuilder()
            var toolCalls: List<ToolCall>? = null
            llmService.sendChatMessageStream(
                messages = chatMessages,
                systemPrompt = systemPrompt,
                tools = tools,
                skipMessagePrefix = true
            ).collect { chunk ->
                if (chunk.content.isNotBlank()) contentBuilder.append(chunk.content)
                if (chunk.toolCalls != null) toolCalls = chunk.toolCalls
            }
            if (toolCalls.isNullOrEmpty()) break
            chatMessages.add(ChatMessage(sessionId = 0, content = contentBuilder.toString(), role = "assistant", toolCalls = toolCalls))
            for (tc in toolCalls!!) {
                val args = parseToolArgs(tc.function.arguments)
                when (tc.function.name) {
                    "update_mood", "update_relationship", "add_important_memory" ->
                        applySceneStateTool(tc, members, scene.townId, scene.day, scene.hour)
                    "finish_scene" -> {
                        args["summary"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }?.let {
                            summary = it.take(SUMMARY_MAX_LENGTH)
                        }
                    }
                }
                chatMessages.add(ChatMessage(sessionId = 0, content = "已记录。", role = "tool", toolCallId = tc.id))
            }
        }

        appendSceneMemory(participants, scene, location, summary)
        townRepository.insertLog(
            TownLogEntry(
                townId = scene.townId, day = scene.day, hour = scene.hour,
                kind = LogKind.TICK, text = "互动场景：$summary"
            )
        )
        val done = scene.copy(status = SceneStatus.DONE, summary = summary)
        townRepository.updateScene(done)
        return Result.success(done)
    }

    // ========== 世界事件 ==========

    /**
     * 玩家注入世界事件：LLM 把一句话简短描述展开为完整事件方案并立即生效。
     */
    suspend fun injectWorldEvent(townId: Long, brief: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val town = townRepository.getTownById(townId)
                ?: return@withContext Result.failure(IllegalStateException("小镇不存在"))
            townRepository.getActiveInteractiveScene(townId)?.let {
                return@withContext Result.failure(IllegalStateException("有进行中的互动场景，请先结束它"))
            }
            val members = townRepository.getMembersByTownIdSync(townId)
            val npcs = members.filter { !it.isPlayerControlled }
            val locations = townRepository.getLocationsByTownIdSync(townId)
            if (locations.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("请先为小镇配置地点"))
            }
            val characters = characterRepository.getCharactersByIds(members.map { it.characterId })
            val nameByMemberId = buildMemberNameMap(members, characters)

            val systemPrompt = buildString {
                appendLine("你是小镇生活模拟的世界事件策划师。玩家会给出一个事件的简短描述，请把它扩展成完整的事件方案。")
                appendLine("## 小镇设定")
                appendLine(town.worldDescription.ifBlank { "（无）" })
                appendLine("## 当前时间")
                appendLine("第${town.currentDay}天 ${town.currentHour}:00")
                appendLine("## 可用地点（ID: 名称）")
                locations.forEach { appendLine("${it.id}: ${it.name} - ${it.description}") }
                appendLine("## 居民（ID: 姓名 | 人设）")
                npcs.forEach { appendLine("${it.id}: ${nameByMemberId[it.id]} | ${it.persona.take(100)}") }
                appendLine("## 要求")
                appendLine("1. 调用一次 create_event_plan 工具提交事件方案；")
                appendLine("2. 事件就在当前时间发生，地点与参与者必须使用给出的ID；")
                appendLine("3. 参与者选择与事件最相关的2-4名居民；")
                appendLine("4. 若事件会改变居民后续安排（如集市日全村去广场），通过 scheduleChangesJson 修改他们的当日行动表，否则传[]。")
            }
            val chatMessages = mutableListOf(
                ChatMessage(sessionId = 0, content = "玩家想要发生的事件：$brief", role = "user")
            )

            var plan: EventPlan? = null
            val maxIterations = userPreferencesRepository.townMaxIterations.first()
            var iteration = 0
            while (iteration < maxIterations && plan == null) {
                iteration++
                val contentBuilder = StringBuilder()
                var toolCalls: List<ToolCall>? = null
                llmService.sendChatMessageStream(
                    messages = chatMessages,
                    systemPrompt = systemPrompt,
                    tools = listOf(createEventPlanTool),
                    skipMessagePrefix = true
                ).collect { chunk ->
                    if (chunk.content.isNotBlank()) contentBuilder.append(chunk.content)
                    if (chunk.toolCalls != null) toolCalls = chunk.toolCalls
                }
                if (toolCalls.isNullOrEmpty()) break
                chatMessages.add(ChatMessage(sessionId = 0, content = contentBuilder.toString(), role = "assistant", toolCalls = toolCalls))
                for (tc in toolCalls!!) {
                    if (tc.function.name != "create_event_plan") continue
                    val args = parseToolArgs(tc.function.arguments)
                    val parsed = parseEventPlan(args, npcs.map { it.id }.toSet(), locations.map { it.id }.toSet())
                    if (parsed != null) plan = parsed
                    chatMessages.add(ChatMessage(sessionId = 0, content = if (parsed != null) "事件方案已记录。" else "事件方案无效，请检查ID后重试。", role = "tool", toolCallId = tc.id))
                }
            }

            val eventPlan = plan
                ?: return@withContext Result.failure(IllegalStateException("事件方案生成失败，请重试"))

            // 应用日程修改
            val locationIds = locations.map { it.id }.toSet()
            for (change in eventPlan.scheduleChanges) {
                val member = npcs.firstOrNull { it.id == change.memberId } ?: continue
                val merged = TownScheduleValidator.mergeChanges(
                    member.todaySchedule, change.items, locationIds, locations.first().id
                )
                townRepository.updateMember(member.copy(todaySchedule = merged))
            }

            townRepository.insertLog(
                TownLogEntry(
                    townId = townId, day = town.currentDay, hour = town.currentHour,
                    kind = LogKind.EVENT, text = "世界事件【${eventPlan.title}】：${eventPlan.description}"
                )
            )

            val scene = TownScene(
                townId = townId,
                day = town.currentDay,
                hour = town.currentHour,
                locationId = eventPlan.locationId,
                type = SceneType.EVENT,
                status = SceneStatus.PENDING,
                participantIds = eventPlan.participantIds
            )
            val sceneId = townRepository.insertScene(scene)
            // 事件场景交给场景页生成（可流式观看）
            Result.success(sceneId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== 快照 ==========

    suspend fun createSnapshot(townId: Long, name: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val town = townRepository.getTownById(townId)
                ?: return@withContext Result.failure(IllegalStateException("小镇不存在"))
            val data = TownSnapshotData(
                town = town,
                locations = townRepository.getLocationsByTownIdSync(townId),
                members = townRepository.getMembersByTownIdSync(townId),
                relationships = townRepository.getRelationshipsByTownIdSync(townId),
                scenes = townRepository.getRecentScenesSync(townId, 100000),
                logs = townRepository.getRecentLogsSync(townId, 100000)
            )
            val snapshotJson = json.encodeToString(TownSnapshotData.serializer(), data)
            val id = townRepository.insertSnapshot(
                TownSnapshot(
                    townId = townId,
                    name = name,
                    day = town.currentDay,
                    hour = town.currentHour,
                    snapshotJson = snapshotJson
                )
            )
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreSnapshot(snapshotId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val snapshot = townRepository.getSnapshotById(snapshotId)
                ?: return@withContext Result.failure(IllegalStateException("存档不存在"))
            val data = json.decodeFromString(TownSnapshotData.serializer(), snapshot.snapshotJson)
            townRepository.restoreSnapshotData(data)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSnapshot(snapshotId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val snapshot = townRepository.getSnapshotById(snapshotId)
                ?: return@withContext Result.failure(IllegalStateException("存档不存在"))
            townRepository.deleteSnapshot(snapshot)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 玩家主动发起互动：在玩家当前位置若有 NPC，则创建等待玩家输入的互动场景。
     * 已有互动场景时返回其 id。返回 null 表示当前地点没有其他角色。
     */
    suspend fun tryStartInteractionAtPlayerLocation(townId: Long): Result<Long?> =
        withContext(Dispatchers.IO) {
            try {
                val town = townRepository.getTownById(townId)
                    ?: return@withContext Result.failure(IllegalStateException("小镇不存在"))
                town.playMemberId
                    ?: return@withContext Result.success(null)
                townRepository.getActiveInteractiveScene(townId)?.let {
                    return@withContext Result.success(it.id)
                }
                val playerMember = townRepository.getMemberById(town.playMemberId)
                    ?: return@withContext Result.success(null)
                val coLocated = townRepository.getMembersByTownIdSync(townId).filter {
                    !it.isPlayerControlled && it.currentLocationId == playerMember.currentLocationId
                }
                if (coLocated.isEmpty()) return@withContext Result.success(null)
                val scene = TownScene(
                    townId = townId,
                    day = town.currentDay,
                    hour = town.currentHour,
                    locationId = playerMember.currentLocationId,
                    type = SceneType.INTERACTION,
                    status = SceneStatus.AWAITING_PLAYER,
                    participantIds = listOf(playerMember.id) + coLocated.map { it.id }
                )
                Result.success(townRepository.insertScene(scene))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ========== 私有实现 ==========

    private class LineStreamParser(private val nameToId: Map<String, Long>) {
        val lines = mutableListOf<SceneLine>()
        private var remainder = StringBuilder()

        fun feed(delta: String): List<SceneLine> {
            remainder.append(delta)
            val out = mutableListOf<SceneLine>()
            while (true) {
                val s = remainder.toString()
                val idx = s.indexOf('\n')
                if (idx < 0) break
                TownSceneParser.parseLine(s.substring(0, idx), nameToId)?.let {
                    lines.add(it)
                    out.add(it)
                }
                remainder = StringBuilder(s.substring(idx + 1))
            }
            return out
        }

        fun finish(): List<SceneLine> {
            val raw = remainder.toString().trim()
            remainder = StringBuilder()
            val line = TownSceneParser.parseLine(raw, nameToId)
            return if (line != null) {
                lines.add(line)
                listOf(line)
            } else {
                emptyList()
            }
        }

        fun currentPartial(): String = remainder.toString().trimStart()
    }

    /**
     * 在应用级作用域中启动场景生成：即使调用方（页面/ViewModel）被销毁，生成仍继续至完成并落库。
     * [onEvent] 在后台协程上回调，仅用于页面在位时的流式呈现（打字机效果）。
     * 返回的 Deferred 以 true 表示场景成功完结（收到 Finished）。
     */
    fun generateSceneInBackground(
        sceneId: Long,
        finalStatus: SceneStatus = SceneStatus.DONE,
        recordMemory: Boolean = true,
        playerInput: String? = null,
        useFinishTool: Boolean = true,
        onEvent: (SceneEvent) -> Unit = {}
    ): Deferred<Boolean> {
        val job = backgroundScope.async {
            var ok = false
            try {
                generateScene(sceneId, finalStatus, recordMemory, playerInput, useFinishTool).collect { event ->
                    if (event is SceneEvent.Finished) ok = true
                    onEvent(event)
                }
            } catch (_: CancellationException) {
            } catch (_: Exception) {
            } finally {
                runningSceneJobs.remove(sceneId)
            }
            ok
        }
        runningSceneJobs[sceneId] = job
        return job
    }

    private val runningSceneJobs = mutableMapOf<Long, Deferred<Boolean>>()

    private suspend fun hasRecentEncounter(
        townId: Long,
        locationId: Long,
        participantIds: List<Long>,
        day: Int,
        hour: Int
    ): Boolean {
        val recent = townRepository.getRecentScenesSync(townId, 10)
        val nowAbs = day * 24 + hour
        return recent.any { scene ->
            scene.type == SceneType.ENCOUNTER &&
                scene.locationId == locationId &&
                scene.participantIds.toSet() == participantIds.toSet() &&
                nowAbs - (scene.day * 24 + scene.hour) <= ENCOUNTER_DEDUPE_HOURS
        }
    }

    private suspend fun applySceneStateTool(
        tc: ToolCall,
        members: List<TownMember>,
        townId: Long,
        day: Int,
        hour: Int
    ): String {
        val args = parseToolArgs(tc.function.arguments)
        fun member(idText: JsonElement?): TownMember? =
            idText?.jsonPrimitive?.content?.toLongOrNull()?.let { mid -> members.firstOrNull { it.id == mid } }
        return when (tc.function.name) {
            "update_mood" -> {
                val m = member(args["memberId"])
                val mood = args["mood"]?.jsonPrimitive?.content ?: ""
                if (m == null || mood.isBlank()) "无效的更新"
                else {
                    townRepository.updateMember(m.copy(mood = mood))
                    "已更新${m.id}的心情。"
                }
            }
            "update_relationship" -> {
                val a = member(args["memberAId"])
                val b = member(args["memberBId"])
                val delta = args["affinityDelta"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val note = args["note"]?.jsonPrimitive?.content ?: ""
                if (a == null || b == null || a.id == b.id) "无效的关系更新"
                else {
                    val existing = townRepository.getRelationshipBetween(townId, a.id, b.id)
                    val newAffinity = ((existing?.affinity ?: 0) + delta).coerceIn(-100, 100)
                    townRepository.upsertRelationship(
                        TownRelationship(
                            townId = townId,
                            memberAId = a.id,
                            memberBId = b.id,
                            affinity = newAffinity,
                            note = note.ifBlank { existing?.note ?: "" }
                        )
                    )
                    "已更新关系。"
                }
            }
            "add_important_memory" -> {
                val m = member(args["memberId"])
                val content = args["content"]?.jsonPrimitive?.content ?: ""
                if (m == null || content.isBlank()) "无效的记忆"
                else {
                    townRepository.updateMember(
                        m.copy(
                            importantMemory = (m.importantMemory + TownMemoryEntry(content, day, hour))
                                .takeLast(MAX_IMPORTANT_MEMORY)
                        )
                    )
                    "已记录重要记忆。"
                }
            }
            else -> "未知工具: ${tc.function.name}"
        }
    }

    private fun parseToolArgs(arguments: String): Map<String, JsonElement> = try {
        json.decodeFromString<Map<String, JsonElement>>(arguments)
    } catch (_: Exception) {
        emptyMap()
    }

    private fun parseEventPlan(
        args: Map<String, JsonElement>,
        validMemberIds: Set<Long>,
        validLocationIds: Set<Long>
    ): EventPlan? {
        val title = args["title"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: return null
        val description = args["description"]?.jsonPrimitive?.content ?: ""
        val locationId = args["locationId"]?.jsonPrimitive?.content?.toLongOrNull()
            ?.takeIf { it in validLocationIds }
        val participantIds = try {
            json.decodeFromString<List<Long>>(args["participantIdsJson"]?.jsonPrimitive?.content ?: "[]")
        } catch (_: Exception) {
            emptyList()
        }.filter { it in validMemberIds }.distinct()
        val scheduleChanges = try {
            json.decodeFromString<List<ScheduleChange>>(
                args["scheduleChangesJson"]?.jsonPrimitive?.content ?: "[]"
            )
        } catch (_: Exception) {
            emptyList()
        }.map { change ->
            change.copy(
                items = change.items.filter { it.endHour > it.startHour && it.locationId in validLocationIds }
            )
        }.filter { it.items.isNotEmpty() && it.memberId in validMemberIds }
        return EventPlan(
            title = title.take(50),
            description = description,
            locationId = locationId,
            participantIds = participantIds,
            scheduleChanges = scheduleChanges
        )
    }

    private fun buildMemberNameMap(
        members: List<TownMember>,
        characters: List<Character>
    ): Map<Long, String> {
        val nameByCharacterId = characters.associate { it.id to it.name }
        return members.associate { it.id to (nameByCharacterId[it.characterId] ?: "未知角色") }
    }

    private fun buildSceneSummary(lines: List<SceneLine>): String {
        val first = lines.firstOrNull { it.kind == "narration" } ?: lines.firstOrNull() ?: return ""
        return first.text.take(SUMMARY_MAX_LENGTH)
    }

    private suspend fun appendSceneMemory(
        participants: List<TownMember>,
        scene: TownScene,
        location: TownLocation?,
        summary: String
    ) {
        val characters = characterRepository.getCharactersByIds(participants.map { it.characterId })
        val nameByMemberId = buildMemberNameMap(participants, characters)
        for (member in participants) {
            val others = participants.filter { it.id != member.id }.mapNotNull { nameByMemberId[it.id] }
            val content = buildString {
                append("第${scene.day}天${scene.hour}点在${location?.name ?: "小镇"}")
                if (others.isNotEmpty()) append("与${others.joinToString("、")}")
                append("：$summary")
            }
            val updated = member.copy(
                recentMemory = (member.recentMemory + TownMemoryEntry(content, scene.day, scene.hour)).takeLast(MAX_RECENT_MEMORY)
            )
            townRepository.updateMember(updated)
        }
    }

    private fun buildScheduleSystemPrompt(
        town: Town,
        npcs: List<TownMember>,
        nameByMemberId: Map<Long, String>,
        locations: List<TownLocation>,
        relationships: List<TownRelationship>
    ): String = buildString {
        appendLine("你是小镇生活模拟的日程规划师。请为小镇「${town.name}」的居民生成第${town.currentDay}天的行动表。")
        if (town.worldDescription.isNotBlank()) {
            appendLine("## 小镇设定")
            appendLine(town.worldDescription)
        }
        appendLine("## 可用地点（locationId: 名称 - 描述）")
        locations.forEach { appendLine("${it.id}: ${it.name} - ${it.description}") }
        appendLine("## 居民")
        npcs.forEach { m ->
            append("- ${nameByMemberId[m.id]}（memberId=${m.id}）：${m.persona.ifBlank { "（无人设）" }}")
            if (m.mood.isNotBlank()) append("；心情：${m.mood}")
            if (m.importantMemory.isNotEmpty()) append("；重要经历：${m.importantMemory.takeLast(3).joinToString("；") { it.content }}")
            appendLine()
        }
        if (relationships.isNotEmpty()) {
            appendLine("## 角色关系")
            relationships.forEach { r ->
                val a = nameByMemberId[r.memberAId] ?: "?"
                val b = nameByMemberId[r.memberBId] ?: "?"
                appendLine("- $a ↔ $b：好感${r.affinity}${if (r.note.isNotBlank()) "（${r.note}）" else ""}")
            }
        }
        appendLine("## 要求")
        appendLine("1. 为每个居民调用一次 set_member_schedule 工具提交当日行动表；")
        appendLine("2. 行动表必须覆盖0-24小时，[startHour,endHour)表示时段，凌晨(22点-次日6点)通常安排休息；")
        appendLine("3. 日程要符合角色身份、性格与近期经历；允许不同角色去同一地点形成交集，也允许各自独立；")
        appendLine("4. activity 用简短短语描述该时段在做什么。")
    }

    private suspend fun buildSceneMessages(
        town: Town,
        location: TownLocation?,
        participants: List<TownMember>,
        nameByMemberId: Map<Long, String>,
        playerMember: TownMember?
    ): Pair<String, MutableList<ChatMessage>> {
        val blocks = userPreferencesRepository.townPromptBlocks.first()
            .filter { it.isEnabled }
            .sortedBy { it.sortOrder }

        val systemParts = mutableListOf<String>()
        val chatMessages = mutableListOf<ChatMessage>()

        for (block in blocks) {
            val role = when (block.type) {
                PromptBlockType.TOWN_SYSTEM_ROLE,
                PromptBlockType.TOWN_WORLD_SETTING,
                PromptBlockType.TOWN_CHARACTERS -> "system"

                PromptBlockType.TOWN_CURRENT_STATE,
                PromptBlockType.TOWN_RECENT_LOGS -> "assistant"

                PromptBlockType.TOWN_OUTPUT_INSTRUCTION -> "user"
                else -> null
            } ?: continue

            val content = when (block.type) {
                PromptBlockType.TOWN_SYSTEM_ROLE ->
                    block.customContent ?: PromptBlockDefaults.defaultContent(block.type)

                PromptBlockType.TOWN_WORLD_SETTING -> buildString {
                    appendLine("## 小镇设定")
                    appendLine(town.name)
                    if (town.worldDescription.isNotBlank()) appendLine(town.worldDescription)
                }.trimEnd()

                PromptBlockType.TOWN_CHARACTERS -> buildString {
                    appendLine("## 本场景角色")
                    participants.forEach { m ->
                        appendLine("- ${nameByMemberId[m.id]}：${m.persona.ifBlank { "（无人设）" }}")
                    }
                }.trimEnd()

                PromptBlockType.TOWN_CURRENT_STATE -> buildString {
                    appendLine("## 当前状态")
                    appendLine("时间：第${town.currentDay}天 ${town.currentHour}:00")
                    appendLine("地点：${location?.name ?: "小镇"}${location?.description?.takeIf { it.isNotBlank() }?.let { "（$it）" } ?: ""}")
                    participants.forEach { m ->
                        appendLine("- ${nameByMemberId[m.id]}：正在${m.currentActivity.ifBlank { "活动" }}，心情${m.mood.ifBlank { "平静" }}")
                        if (m.recentMemory.isNotEmpty()) {
                            appendLine("  近期记忆：${m.recentMemory.takeLast(3).joinToString("；") { it.content }}")
                        }
                        if (m.importantMemory.isNotEmpty()) {
                            appendLine("  重要记忆：${m.importantMemory.takeLast(3).joinToString("；") { it.content }}")
                        }
                    }
                    val relations = participants.toList()
                        .flatMap { a -> participants.map { a to it } }
                        .filter { (a, b) -> a.id < b.id }
                    relations.forEach { (a, b) ->
                        val rel = townRepository.getRelationshipBetween(town.id, a.id, b.id)
                        if (rel != null) {
                            appendLine("- ${nameByMemberId[a.id]} ↔ ${nameByMemberId[b.id]}：好感${rel.affinity}${if (rel.note.isNotBlank()) "（${rel.note}）" else ""}")
                        }
                    }
                    if (playerMember != null) {
                        appendLine("注意：${nameByMemberId[playerMember.id]}由玩家扮演。")
                    }
                }.trimEnd()

                PromptBlockType.TOWN_RECENT_LOGS -> {
                    val logs = townRepository.getRecentLogsSync(town.id, CONTEXT_LOG_COUNT)
                    if (logs.isEmpty()) null
                    else {
                        val text = logs.joinToString("\n") { "第${it.day}天${it.hour}:00 ${it.text}" }
                        val truncated = if (text.length > town.windowWordCount) "…" + text.takeLast(town.windowWordCount) else text
                        "## 小镇近况日志\n$truncated"
                    }
                }

                PromptBlockType.TOWN_OUTPUT_INSTRUCTION ->
                    block.customContent ?: PromptBlockDefaults.defaultContent(block.type)

                else -> null
            }

            if (content.isNullOrBlank()) continue
            if (role == "system") {
                systemParts.add(content)
            } else {
                chatMessages.add(ChatMessage(sessionId = 0, content = content, role = role))
            }
        }
        return systemParts.joinToString("\n\n") to chatMessages
    }

    private fun buildSceneTask(
        scene: TownScene,
        location: TownLocation?,
        nameByMemberId: Map<Long, String>,
        playerMember: TownMember?
    ): String = buildString {
        appendLine("请演绎以下场景：")
        appendLine("时间：第${scene.day}天 ${scene.hour}:00")
        appendLine("地点：${location?.name ?: "小镇"}")
        appendLine("参与角色：${scene.participantIds.mapNotNull { nameByMemberId[it] }.joinToString("、")}")
        val typeDesc = when (scene.type) {
            SceneType.ENCOUNTER -> "角色们在同一地点不期而遇"
            SceneType.INTERACTION -> "与玩家的互动场景"
            SceneType.EVENT -> "一个正在发生的事件"
        }
        appendLine("场景类型：$typeDesc")
        if (playerMember != null) {
            appendLine("注意：${nameByMemberId[playerMember.id]}由玩家扮演，不要生成该角色的台词，剧情自然地为其留出空间。")
        }
        append("请开始演绎这一场景。")
    }.trimEnd()
}
