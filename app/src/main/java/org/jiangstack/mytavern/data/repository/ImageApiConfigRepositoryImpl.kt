package org.jiangstack.mytavern.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jiangstack.mytavern.data.local.dao.ImageApiConfigDao
import org.jiangstack.mytavern.data.local.entity.ImageApiConfigEntity
import org.jiangstack.mytavern.domain.model.ImageApiConfig
import org.jiangstack.mytavern.domain.repository.ImageApiConfigRepository

class ImageApiConfigRepositoryImpl(
    private val imageApiConfigDao: ImageApiConfigDao
) : ImageApiConfigRepository {

    override fun getAllConfigs(): Flow<List<ImageApiConfig>> {
        return imageApiConfigDao.getAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getConfigById(id: Long): ImageApiConfig? {
        return imageApiConfigDao.getById(id)?.toDomain()
    }

    override suspend fun insertConfig(config: ImageApiConfig): Long {
        return imageApiConfigDao.insert(config.toEntity())
    }

    override suspend fun updateConfig(config: ImageApiConfig) {
        imageApiConfigDao.update(config.toEntity())
    }

    override suspend fun deleteConfig(config: ImageApiConfig) {
        imageApiConfigDao.delete(config.toEntity())
    }

    private fun ImageApiConfigEntity.toDomain() = ImageApiConfig(
        id = id,
        name = name,
        baseUrl = baseUrl,
        apiKey = apiKey,
        model = model
    )

    private fun ImageApiConfig.toEntity() = ImageApiConfigEntity(
        id = id,
        name = name,
        baseUrl = baseUrl,
        apiKey = apiKey,
        model = model
    )
}
