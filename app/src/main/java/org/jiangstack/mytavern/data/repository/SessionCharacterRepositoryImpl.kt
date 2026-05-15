package org.jiangstack.mytavern.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jiangstack.mytavern.data.local.dao.SessionCharacterDao
import org.jiangstack.mytavern.data.local.entity.SessionCharacterEntity
import org.jiangstack.mytavern.domain.repository.SessionCharacterRepository

class SessionCharacterRepositoryImpl(
    private val sessionCharacterDao: SessionCharacterDao
) : SessionCharacterRepository {
    override fun getCharactersBySessionId(sessionId: Long): Flow<List<Long>> =
        sessionCharacterDao.getBySessionId(sessionId)
            .map { list -> list.map { it.characterId } }

    override suspend fun addCharacterToSession(sessionId: Long, characterId: Long) {
        sessionCharacterDao.insert(SessionCharacterEntity(sessionId, characterId))
    }

    override suspend fun addCharactersToSession(sessionId: Long, characterIds: List<Long>) {
        sessionCharacterDao.insertAll(
            characterIds.map { SessionCharacterEntity(sessionId, it) }
        )
    }

    override suspend fun clearSessionCharacters(sessionId: Long) {
        sessionCharacterDao.deleteBySessionId(sessionId)
    }
}
