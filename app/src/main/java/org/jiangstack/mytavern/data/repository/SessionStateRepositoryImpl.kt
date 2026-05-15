package org.jiangstack.mytavern.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jiangstack.mytavern.data.local.dao.SessionStateDao
import org.jiangstack.mytavern.data.local.entity.SessionStateEntity
import org.jiangstack.mytavern.domain.model.SessionState
import org.jiangstack.mytavern.domain.repository.SessionStateRepository

class SessionStateRepositoryImpl(
    private val sessionStateDao: SessionStateDao
) : SessionStateRepository {

    override fun getBySessionId(sessionId: Long): Flow<List<SessionState>> {
        return sessionStateDao.getBySessionId(sessionId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun insertOrUpdate(sessionId: Long, key: String, value: String) {
        sessionStateDao.insertOrUpdate(
            SessionStateEntity(
                sessionId = sessionId,
                stateKey = key,
                stateValue = value,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun delete(sessionId: Long, key: String) {
        sessionStateDao.delete(sessionId, key)
    }

    private fun SessionStateEntity.toDomain() = SessionState(
        sessionId = sessionId,
        key = stateKey,
        value = stateValue,
        updatedAt = updatedAt
    )
}
