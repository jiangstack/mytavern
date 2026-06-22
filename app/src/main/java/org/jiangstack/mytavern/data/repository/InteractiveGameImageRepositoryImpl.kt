package org.jiangstack.mytavern.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jiangstack.mytavern.data.local.dao.InteractiveGameImageDao
import org.jiangstack.mytavern.data.local.entity.InteractiveGameImageEntity
import org.jiangstack.mytavern.domain.model.InteractiveGameImage
import org.jiangstack.mytavern.domain.repository.InteractiveGameImageRepository

class InteractiveGameImageRepositoryImpl(
    private val imageDao: InteractiveGameImageDao
) : InteractiveGameImageRepository {

    override fun getImagesByGameId(gameId: Long): Flow<List<InteractiveGameImage>> {
        return imageDao.getByGameId(gameId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getImageById(id: Long): InteractiveGameImage? {
        return imageDao.getById(id)?.toDomain()
    }

    override suspend fun insertImage(image: InteractiveGameImage): Long {
        return imageDao.insert(image.toEntity())
    }

    override suspend fun deleteImage(image: InteractiveGameImage) {
        imageDao.delete(image.toEntity())
    }

    private fun InteractiveGameImageEntity.toDomain() = InteractiveGameImage(
        id = id,
        gameId = gameId,
        remoteUrl = remoteUrl,
        localUri = localUri,
        createdAt = createdAt
    )

    private fun InteractiveGameImage.toEntity() = InteractiveGameImageEntity(
        id = id,
        gameId = gameId,
        remoteUrl = remoteUrl,
        localUri = localUri,
        createdAt = createdAt
    )
}
