package org.jiangstack.mytavern.domain.repository

import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.domain.model.SessionState

interface SessionStateRepository {
    fun getBySessionId(sessionId: Long): Flow<List<SessionState>>
    suspend fun insertOrUpdate(sessionId: Long, key: String, value: String)
    suspend fun delete(sessionId: Long, key: String)
}
