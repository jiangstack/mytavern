package org.jiangstack.mytavern.domain.repository

import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.domain.model.ImageApiConfig

interface ImageApiConfigRepository {
    fun getAllConfigs(): Flow<List<ImageApiConfig>>
    suspend fun getConfigById(id: Long): ImageApiConfig?
    suspend fun insertConfig(config: ImageApiConfig): Long
    suspend fun updateConfig(config: ImageApiConfig)
    suspend fun deleteConfig(config: ImageApiConfig)
}
