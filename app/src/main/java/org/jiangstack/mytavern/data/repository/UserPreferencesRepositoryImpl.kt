package org.jiangstack.mytavern.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jiangstack.mytavern.domain.model.ThemeMode
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepositoryImpl(
    private val context: Context
) : UserPreferencesRepository {

    private val defaultUserCharacterIdKey = longPreferencesKey("default_user_character_id")
    private val defaultLlmConfigIdKey = longPreferencesKey("default_llm_config_id")
    private val themeModeKey = intPreferencesKey("theme_mode")
    private val chatHistoryCountKey = intPreferencesKey("chat_history_count")
    private val temperatureKey = floatPreferencesKey("temperature")
    private val maxTokensKey = intPreferencesKey("max_tokens")

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
}
