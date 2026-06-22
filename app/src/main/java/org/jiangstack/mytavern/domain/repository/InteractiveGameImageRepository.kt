package org.jiangstack.mytavern.domain.repository

import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.domain.model.InteractiveGameImage

interface InteractiveGameImageRepository {
    fun getImagesByGameId(gameId: Long): Flow<List<InteractiveGameImage>>
    suspend fun getImageById(id: Long): InteractiveGameImage?
    suspend fun insertImage(image: InteractiveGameImage): Long
    suspend fun deleteImage(image: InteractiveGameImage)
}
