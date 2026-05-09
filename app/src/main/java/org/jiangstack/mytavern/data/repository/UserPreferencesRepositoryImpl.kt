package org.jiangstack.mytavern.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepositoryImpl(
    private val context: Context
) : UserPreferencesRepository {

    private val defaultUserCharacterIdKey = longPreferencesKey("default_user_character_id")
    private val defaultLlmConfigIdKey = longPreferencesKey("default_llm_config_id")

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
}
