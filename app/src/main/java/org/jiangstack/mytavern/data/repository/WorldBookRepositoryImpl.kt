package org.jiangstack.mytavern.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jiangstack.mytavern.data.local.dao.WorldBookDao
import org.jiangstack.mytavern.data.local.dao.WorldBookRuleDao
import org.jiangstack.mytavern.data.local.entity.WorldBookEntity
import org.jiangstack.mytavern.data.local.entity.WorldBookRuleEntity
import org.jiangstack.mytavern.domain.model.WorldBook
import org.jiangstack.mytavern.domain.model.WorldBookRule
import org.jiangstack.mytavern.domain.repository.WorldBookRepository

class WorldBookRepositoryImpl(
    private val worldBookDao: WorldBookDao,
    private val worldBookRuleDao: WorldBookRuleDao
) : WorldBookRepository {

    override fun getAllWorldBooks(): Flow<List<WorldBook>> {
        return worldBookDao.getAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getWorldBookById(id: Long): WorldBook? {
        val entity = worldBookDao.getById(id) ?: return null
        val rules = getRulesByWorldBookIdSync(id)
        return entity.toDomain().copy(rules = rules)
    }

    override suspend fun insertWorldBook(worldBook: WorldBook): Long {
        return worldBookDao.insert(worldBook.toEntity())
    }

    override suspend fun updateWorldBook(worldBook: WorldBook) {
        worldBookDao.update(worldBook.toEntity())
    }

    override suspend fun deleteWorldBook(worldBook: WorldBook) {
        worldBookDao.delete(worldBook.toEntity())
    }

    override fun getRulesByWorldBookId(worldBookId: Long): Flow<List<WorldBookRule>> {
        return worldBookRuleDao.getByWorldBookId(worldBookId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getRulesByWorldBookIdSync(worldBookId: Long): List<WorldBookRule> {
        return worldBookRuleDao.getByWorldBookIdSync(worldBookId).map { it.toDomain() }
    }

    override suspend fun insertRule(rule: WorldBookRule): Long {
        return worldBookRuleDao.insert(rule.toEntity())
    }

    override suspend fun updateRule(rule: WorldBookRule) {
        worldBookRuleDao.update(rule.toEntity())
    }

    override suspend fun deleteRule(rule: WorldBookRule) {
        worldBookRuleDao.delete(rule.toEntity())
    }

    private fun WorldBookEntity.toDomain() = WorldBook(
        id = id,
        name = name,
        description = description
    )

    private fun WorldBook.toEntity() = WorldBookEntity(
        id = id,
        name = name,
        description = description
    )

    private fun WorldBookRuleEntity.toDomain() = WorldBookRule(
        id = id,
        worldBookId = worldBookId,
        name = name,
        description = description,
        matchType = matchType
    )

    private fun WorldBookRule.toEntity() = WorldBookRuleEntity(
        id = id,
        worldBookId = worldBookId,
        name = name,
        description = description,
        matchType = matchType
    )
}
