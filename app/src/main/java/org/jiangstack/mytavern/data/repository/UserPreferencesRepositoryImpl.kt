package org.jiangstack.mytavern.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.jiangstack.mytavern.domain.model.PromptBlockConfig
import org.jiangstack.mytavern.domain.model.PromptBlockDefaults
import org.jiangstack.mytavern.domain.model.ThemeMode
import org.jiangstack.mytavern.domain.model.PromptBlockType

import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepositoryImpl(
    private val context: Context
) : UserPreferencesRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }


    /**
     * 合并已保存的分块配置与默认配置，确保新增的默认分块类型始终存在。
     * 保留用户已有的自定义内容和顺序，仅补充缺失的分块。
     */
    private fun mergeBlocksWithDefaults(
        saved: List<PromptBlockConfig>,
        defaults: List<PromptBlockConfig>
    ): List<PromptBlockConfig> {
        val savedTypes = saved.map { it.type }.toSet()
        val missingDefaults = defaults.filter { it.type !in savedTypes }
        if (missingDefaults.isEmpty()) return saved
        return saved + missingDefaults
    }
    private val defaultUserCharacterIdKey = longPreferencesKey("default_user_character_id")
    private val defaultLlmConfigIdKey = longPreferencesKey("default_llm_config_id")
    private val themeModeKey = intPreferencesKey("theme_mode")
    private val chatHistoryCountKey = intPreferencesKey("chat_history_count")
    private val temperatureKey = floatPreferencesKey("temperature")
    private val maxTokensKey = intPreferencesKey("max_tokens")
    private val novelPromptBlocksKey = stringPreferencesKey("novel_prompt_blocks")
    private val novelModifyPromptBlocksKey = stringPreferencesKey("novel_modify_prompt_blocks")
    private val novelOutlinePromptBlocksKey = stringPreferencesKey("novel_outline_prompt_blocks")
    private val dialogueHighlightEnabledKey = booleanPreferencesKey("dialogue_highlight_enabled")
    private val dialogueHighlightColorKey = longPreferencesKey("dialogue_highlight_color")

    override val defaultUserCharacterId: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[defaultUserCharacterIdKey]
        }

    override suspend fun setDefaultUserCharacterId(id: Long?) {
        context.dataStore.edit { preferences ->
            if (id != null) {
                preferences[defaultUserCharacterIdKey] = id
            } else {
                preferences.remove(defaultUserCharacterIdKey)
            }
        }
    }

    override val defaultLlmConfigId: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[defaultLlmConfigIdKey]
        }

    override suspend fun setDefaultLlmConfigId(id: Long?) {
        context.dataStore.edit { preferences ->
            if (id != null) {
                preferences[defaultLlmConfigIdKey] = id
            } else {
                preferences.remove(defaultLlmConfigIdKey)
            }
        }
    }

    override val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            val ordinal = preferences[themeModeKey]
            if (ordinal != null) {
                ThemeMode.entries.getOrElse(ordinal) { ThemeMode.SYSTEM }
            } else {
                ThemeMode.SYSTEM
            }
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[themeModeKey] = mode.ordinal
        }
    }

    override val chatHistoryCount: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[chatHistoryCountKey] ?: 12
        }

    override suspend fun setChatHistoryCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[chatHistoryCountKey] = count.coerceIn(1, 50)
        }
    }

    override val temperature: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[temperatureKey] ?: 1.0f
        }

    override suspend fun setTemperature(temp: Float) {
        context.dataStore.edit { preferences ->
            preferences[temperatureKey] = temp.coerceIn(0.0f, 2.0f)
        }
    }

    override val maxTokens: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[maxTokensKey] ?: 4096
        }

    override suspend fun setMaxTokens(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[maxTokensKey] = value.coerceIn(256, 32768)
        }
    }

    override val novelPromptBlocks: Flow<List<PromptBlockConfig>> = context.dataStore.data
        .map { preferences ->
            val jsonStr = preferences[novelPromptBlocksKey]
            val defaults = PromptBlockDefaults.continueWritingBlocks()
            val saved = if (jsonStr != null) {
                try {
                    json.decodeFromString(ListSerializer(PromptBlockConfig.serializer()), jsonStr)
                } catch (_: Exception) {
                    defaults
                }
            } else {
                defaults
            }
            mergeBlocksWithDefaults(saved, defaults)
        }

    override suspend fun setNovelPromptBlocks(blocks: List<PromptBlockConfig>) {
        context.dataStore.edit { preferences ->
            preferences[novelPromptBlocksKey] =
                json.encodeToString(ListSerializer(PromptBlockConfig.serializer()), blocks)
        }
    }

    override val novelModifyPromptBlocks: Flow<List<PromptBlockConfig>> = context.dataStore.data
        .map { preferences ->
            val jsonStr = preferences[novelModifyPromptBlocksKey]
            val defaults = PromptBlockDefaults.modifyBlocks()
            val saved = if (jsonStr != null) {
                try {
                    json.decodeFromString(ListSerializer(PromptBlockConfig.serializer()), jsonStr)
                } catch (_: Exception) {
                    defaults
                }
            } else {
                defaults
            }
            mergeBlocksWithDefaults(saved, defaults)
        }

    override suspend fun setNovelModifyPromptBlocks(blocks: List<PromptBlockConfig>) {
        context.dataStore.edit { preferences ->
            preferences[novelModifyPromptBlocksKey] =
                json.encodeToString(ListSerializer(PromptBlockConfig.serializer()), blocks)
        }
    }

    override val novelOutlinePromptBlocks: Flow<List<PromptBlockConfig>> = context.dataStore.data
        .map { preferences ->
            val jsonStr = preferences[novelOutlinePromptBlocksKey]
            val defaults = PromptBlockDefaults.outlineBlocks()
            val saved = if (jsonStr != null) {
                try {
                    json.decodeFromString(ListSerializer(PromptBlockConfig.serializer()), jsonStr)
                } catch (_: Exception) {
                    defaults
                }
            } else {
                defaults
            }
            mergeBlocksWithDefaults(saved, defaults)
        }

    override suspend fun setNovelOutlinePromptBlocks(blocks: List<PromptBlockConfig>) {
        context.dataStore.edit { preferences ->
            preferences[novelOutlinePromptBlocksKey] =
                json.encodeToString(ListSerializer(PromptBlockConfig.serializer()), blocks)
        }
    }

    override val dialogueHighlightEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[dialogueHighlightEnabledKey] ?: true
        }

    override suspend fun setDialogueHighlightEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[dialogueHighlightEnabledKey] = enabled
        }
    }

    override val dialogueHighlightColor: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[dialogueHighlightColorKey] ?: 0xFF4FC3F7L
        }

    override suspend fun setDialogueHighlightColor(color: Long) {
        context.dataStore.edit { preferences ->
            preferences[dialogueHighlightColorKey] = color
        }
    }
}
