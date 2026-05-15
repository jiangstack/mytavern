package org.jiangstack.mytavern.domain.repository

import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.domain.model.ChatMessage
import org.jiangstack.mytavern.domain.model.ChatSession

interface ChatRepository {
    fun getAllSessions(): Flow<List<ChatSession>>
    suspend fun getSessionById(id: Long): ChatSession?
    suspend fun insertSession(session: ChatSession): Long
    suspend fun updateSession(session: ChatSession)
    suspend fun deleteSession(session: ChatSession)

    fun getMessagesBySessionId(sessionId: Long): Flow<List<ChatMessage>>
    suspend fun insertMessage(message: ChatMessage): Long
    suspend fun updateMessage(message: ChatMessage)
    suspend fun deleteMessage(message: ChatMessage)
    suspend fun deleteMessagesAfter(sessionId: Long, timestamp: Long)
    suspend fun deleteMessagesBySessionId(sessionId: Long)
}
