package org.jiangstack.mytavern.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jiangstack.mytavern.data.local.dao.QuickReplyDao
import org.jiangstack.mytavern.data.local.entity.QuickReplyEntity
import org.jiangstack.mytavern.domain.model.QuickReply
import org.jiangstack.mytavern.domain.repository.QuickReplyRepository

class QuickReplyRepositoryImpl(
    private val quickReplyDao: QuickReplyDao
) : QuickReplyRepository {

    override fun getAll(): Flow<List<QuickReply>> {
        return quickReplyDao.getAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getById(id: Long): QuickReply? {
        return quickReplyDao.getById(id)?.toDomain()
    }

    override suspend fun insert(quickReply: QuickReply): Long {
        return quickReplyDao.insert(quickReply.toEntity())
    }

    override suspend fun update(quickReply: QuickReply) {
        quickReplyDao.update(quickReply.toEntity())
    }

    override suspend fun delete(quickReply: QuickReply) {
        quickReplyDao.delete(quickReply.toEntity())
    }

    private fun QuickReplyEntity.toDomain() = QuickReply(
        id = id,
        label = label,
        message = message
    )

    private fun QuickReply.toEntity() = QuickReplyEntity(
        id = id,
        label = label,
        message = message
    )
}
