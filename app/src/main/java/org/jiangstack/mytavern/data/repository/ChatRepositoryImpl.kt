package org.jiangstack.mytavern.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jiangstack.mytavern.data.local.dao.ChatMessageDao
import org.jiangstack.mytavern.data.local.dao.ChatSessionDao
import org.jiangstack.mytavern.data.local.entity.ChatMessageEntity
import org.jiangstack.mytavern.data.local.entity.ChatSessionEntity
import org.jiangstack.mytavern.domain.model.ChatMessage
import org.jiangstack.mytavern.domain.model.ChatSession
import org.jiangstack.mytavern.domain.model.SessionType
import org.jiangstack.mytavern.domain.repository.ChatRepository

class ChatRepositoryImpl(
    private val chatSessionDao: ChatSessionDao,
    private val chatMessageDao: ChatMessageDao
) : ChatRepository {

    override fun getAllSessions(): Flow<List<ChatSession>> {
        return chatSessionDao.getAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getSessionById(id: Long): ChatSession? {
        return chatSessionDao.getById(id)?.toDomain()
    }

    override suspend fun insertSession(session: ChatSession): Long {
        return chatSessionDao.insert(session.toEntity())
    }

    override suspend fun updateSession(session: ChatSession) {
        chatSessionDao.update(session.toEntity())
    }

    override suspend fun deleteSession(session: ChatSession) {
        chatSessionDao.delete(session.toEntity())
    }

    override fun getMessagesBySessionId(sessionId: Long): Flow<List<ChatMessage>> {
        return chatMessageDao.getBySessionId(sessionId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun insertMessage(message: ChatMessage): Long {
        return chatMessageDao.insert(message.toEntity())
    }

    override suspend fun updateMessage(message: ChatMessage) {
        chatMessageDao.update(message.toEntity())
    }

    override suspend fun deleteMessage(message: ChatMessage) {
        chatMessageDao.delete(message.toEntity())
    }

    override suspend fun deleteMessagesAfter(sessionId: Long, timestamp: Long) {
        chatMessageDao.deleteBySessionIdAndTimestampAfter(sessionId, timestamp)
    }

    override suspend fun deleteMessagesBySessionId(sessionId: Long) {
        chatMessageDao.deleteBySessionId(sessionId)
    }

    private fun ChatSessionEntity.toDomain() = ChatSession(
        id = id,
        type = SessionType.valueOf(type),
        title = title,
        backgroundUri = backgroundUri,
        userCharacterId = userCharacterId,
        aiCharacterId = aiCharacterId,
        worldBookId = worldBookId,
        sessionStateEnabled = sessionStateEnabled,
        createdAt = createdAt
    )

    private fun ChatSession.toEntity() = ChatSessionEntity(
        id = id,
        type = type.name,
        title = title,
        backgroundUri = backgroundUri,
        userCharacterId = userCharacterId,
        aiCharacterId = aiCharacterId,
        worldBookId = worldBookId,
        sessionStateEnabled = sessionStateEnabled,
        createdAt = createdAt
    )

    private fun ChatMessageEntity.toDomain() = ChatMessage(
        id = id,
        sessionId = sessionId,
        senderId = senderId,
        senderName = senderName,
        content = content,
        timestamp = timestamp,
        parentMessageId = parentMessageId,
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens
    )

    private fun ChatMessage.toEntity() = ChatMessageEntity(
        id = id,
        sessionId = sessionId,
        senderId = senderId,
        senderName = senderName,
        content = content,
        timestamp = timestamp,
        parentMessageId = parentMessageId,
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens
    )
}
