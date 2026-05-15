package org.jiangstack.mytavern.domain.repository

import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.domain.model.QuickReply

interface QuickReplyRepository {
    fun getAll(): Flow<List<QuickReply>>
    suspend fun getById(id: Long): QuickReply?
    suspend fun insert(quickReply: QuickReply): Long
    suspend fun update(quickReply: QuickReply)
    suspend fun delete(quickReply: QuickReply)
}
