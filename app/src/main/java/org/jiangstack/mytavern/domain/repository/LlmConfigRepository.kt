package org.jiangstack.mytavern.domain.repository

import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.domain.model.LlmConfig

interface LlmConfigRepository {
    fun getAllConfigs(): Flow<List<LlmConfig>>
    suspend fun getConfigById(id: Long): LlmConfig?
    suspend fun insertConfig(config: LlmConfig): Long
    suspend fun updateConfig(config: LlmConfig)
    suspend fun deleteConfig(config: LlmConfig)
}
