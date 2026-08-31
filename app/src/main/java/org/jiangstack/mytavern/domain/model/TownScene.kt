package org.jiangstack.mytavern.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TownScene(
    val id: Long = 0,
    val townId: Long,
    val day: Int,
    val hour: Int,
    val locationId: Long? = null,
    val type: SceneType = SceneType.ENCOUNTER,
    val status: SceneStatus = SceneStatus.PENDING,
    val participantIds: List<Long> = emptyList(),
    val lines: List<SceneLine> = emptyList(),
    val summary: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * AVG 台词行。kind: narration(旁白) / dialogue(对话) / action(动作) / thought(心理)。
 */
@Serializable
data class SceneLine(
    val kind: String,
    val speakerId: Long? = null,
    val speakerName: String? = null,
    val text: String
)

@Serializable
enum class SceneType { ENCOUNTER, INTERACTION, EVENT }

@Serializable
enum class SceneStatus { PENDING, GENERATING, DONE, INTERACTIVE, AWAITING_PLAYER }
