package org.jiangstack.mytavern.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val defaultUserCharacterId: Flow<Long?>
    suspend fun setDefaultUserCharacterId(id: Long?)
}
