package org.jiangstack.mytavern.domain.repository

import kotlinx.coroutines.flow.Flow

interface SessionCharacterRepository {
    fun getCharactersBySessionId(sessionId: Long): Flow<List<Long>>
    suspend fun addCharacterToSession(sessionId: Long, characterId: Long)
    suspend fun addCharactersToSession(sessionId: Long, characterIds: List<Long>)
    suspend fun clearSessionCharacters(sessionId: Long)
}
