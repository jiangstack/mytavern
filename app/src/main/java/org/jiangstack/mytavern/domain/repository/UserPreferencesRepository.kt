package org.jiangstack.mytavern.domain.repository

import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.domain.model.ThemeMode

interface UserPreferencesRepository {
    val defaultUserCharacterId: Flow<Long?>
    suspend fun setDefaultUserCharacterId(id: Long?)
    val defaultLlmConfigId: Flow<Long?>
    suspend fun setDefaultLlmConfigId(id: Long?)
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    val chatHistoryCount: Flow<Int>
    suspend fun setChatHistoryCount(count: Int)
    val temperature: Flow<Float>
    suspend fun setTemperature(temp: Float)
}
