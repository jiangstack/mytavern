package org.jiangstack.mytavern.domain.repository

import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.domain.model.WorldBook
import org.jiangstack.mytavern.domain.model.WorldBookRule

interface WorldBookRepository {
    fun getAllWorldBooks(): Flow<List<WorldBook>>
    suspend fun getWorldBookById(id: Long): WorldBook?
    suspend fun insertWorldBook(worldBook: WorldBook): Long
    suspend fun updateWorldBook(worldBook: WorldBook)
    suspend fun deleteWorldBook(worldBook: WorldBook)

    suspend fun copyWorldBook(worldBookId: Long): Long

    fun getRulesByWorldBookId(worldBookId: Long): Flow<List<WorldBookRule>>
    suspend fun getRulesByWorldBookIdSync(worldBookId: Long): List<WorldBookRule>
    suspend fun insertRule(rule: WorldBookRule): Long
    suspend fun updateRule(rule: WorldBookRule)
    suspend fun deleteRule(rule: WorldBookRule)
}
