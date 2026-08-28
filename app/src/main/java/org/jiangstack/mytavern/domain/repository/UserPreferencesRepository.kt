package org.jiangstack.mytavern.domain.repository

import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.domain.model.PromptBlockConfig
import org.jiangstack.mytavern.domain.model.ThemeMode

interface UserPreferencesRepository {
    val defaultUserCharacterId: Flow<Long?>
    suspend fun setDefaultUserCharacterId(id: Long?)
    val defaultLlmConfigId: Flow<Long?>
    suspend fun setDefaultLlmConfigId(id: Long?)
    val defaultImageApiConfigId: Flow<Long?>
    suspend fun setDefaultImageApiConfigId(id: Long?)
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    val chatHistoryCount: Flow<Int>
    suspend fun setChatHistoryCount(count: Int)
    val temperature: Flow<Float>
    suspend fun setTemperature(temp: Float)
    val maxTokens: Flow<Int>
    suspend fun setMaxTokens(value: Int)
    val novelPromptBlocks: Flow<List<PromptBlockConfig>>
    suspend fun setNovelPromptBlocks(blocks: List<PromptBlockConfig>)
    val novelModifyPromptBlocks: Flow<List<PromptBlockConfig>>
    suspend fun setNovelModifyPromptBlocks(blocks: List<PromptBlockConfig>)
    val novelOutlinePromptBlocks: Flow<List<PromptBlockConfig>>
    suspend fun setNovelOutlinePromptBlocks(blocks: List<PromptBlockConfig>)
    val interactivePromptBlocks: Flow<List<PromptBlockConfig>>
    suspend fun setInteractivePromptBlocks(blocks: List<PromptBlockConfig>)
    val interactiveMaxIterations: Flow<Int>
    suspend fun setInteractiveMaxIterations(count: Int)
    val dialogueHighlightEnabled: Flow<Boolean>
    suspend fun setDialogueHighlightEnabled(enabled: Boolean)
    val dialogueHighlightColor: Flow<Long>
    suspend fun setDialogueHighlightColor(color: Long)
    val lastImageGenPrompt: Flow<String?>
    suspend fun saveLastImageGenPrompt(prompt: String?)
    val lastImageGenParams: Flow<String?>
    suspend fun saveLastImageGenParams(params: String?)
}
