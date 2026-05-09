package org.jiangstack.mytavern.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jiangstack.mytavern.data.local.dao.LlmConfigDao
import org.jiangstack.mytavern.data.local.entity.LlmConfigEntity
import org.jiangstack.mytavern.domain.model.ApiType
import org.jiangstack.mytavern.domain.model.LlmConfig
import org.jiangstack.mytavern.domain.repository.LlmConfigRepository

class LlmConfigRepositoryImpl(
    private val llmConfigDao: LlmConfigDao
) : LlmConfigRepository {

    override fun getAllConfigs(): Flow<List<LlmConfig>> {
        return llmConfigDao.getAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getConfigById(id: Long): LlmConfig? {
        return llmConfigDao.getById(id)?.toDomain()
    }

    override suspend fun insertConfig(config: LlmConfig): Long {
        return llmConfigDao.insert(config.toEntity())
    }

    override suspend fun updateConfig(config: LlmConfig) {
        llmConfigDao.update(config.toEntity())
    }

    override suspend fun deleteConfig(config: LlmConfig) {
        llmConfigDao.delete(config.toEntity())
    }

    private fun LlmConfigEntity.toDomain() = LlmConfig(
        id = id,
        name = name,
        apiType = ApiType.valueOf(apiType),
        baseUrl = baseUrl,
        apiKey = apiKey,
        model = model
    )

    private fun LlmConfig.toEntity() = LlmConfigEntity(
        id = id,
        name = name,
        apiType = apiType.name,
        baseUrl = baseUrl,
        apiKey = apiKey,
        model = model
    )
}
