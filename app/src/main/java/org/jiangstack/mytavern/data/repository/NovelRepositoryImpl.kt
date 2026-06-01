package org.jiangstack.mytavern.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jiangstack.mytavern.data.local.dao.NovelChapterDao
import org.jiangstack.mytavern.data.local.dao.NovelCharacterDao
import org.jiangstack.mytavern.data.local.dao.NovelDao
import org.jiangstack.mytavern.data.local.entity.NovelChapterEntity
import org.jiangstack.mytavern.data.local.entity.NovelCharacterEntity
import org.jiangstack.mytavern.data.local.entity.NovelEntity
import org.jiangstack.mytavern.domain.model.Novel
import org.jiangstack.mytavern.domain.model.NovelChapter
import org.jiangstack.mytavern.domain.repository.NovelRepository

class NovelRepositoryImpl(
    private val novelDao: NovelDao,
    private val novelChapterDao: NovelChapterDao,
    private val novelCharacterDao: NovelCharacterDao
) : NovelRepository {

    // ========== 小说 ==========

    override fun getAllNovels(): Flow<List<Novel>> {
        return novelDao.getAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getNovelById(id: Long): Novel? {
        val entity = novelDao.getById(id) ?: return null
        val characterIds = getCharacterIdsByNovelIdSync(id)
        return entity.toDomain().copy(characterIds = characterIds)
    }

    override suspend fun insertNovel(novel: Novel): Long {
        val id = novelDao.insert(novel.toEntity())
        if (novel.characterIds.isNotEmpty()) {
            novelCharacterDao.insertAll(
                novel.characterIds.map { NovelCharacterEntity(id, it) }
            )
        }
        return id
    }

    override suspend fun updateNovel(novel: Novel) {
        novelDao.update(novel.toEntity().copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteNovel(novel: Novel) {
        novelDao.delete(novel.toEntity())
    }

    // ========== 章节 ==========

    override fun getChaptersByNovelId(novelId: Long): Flow<List<NovelChapter>> {
        return novelChapterDao.getByNovelId(novelId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getChaptersByNovelIdSync(novelId: Long): List<NovelChapter> {
        return novelChapterDao.getByNovelIdSync(novelId).map { it.toDomain() }
    }

    override suspend fun getChapterById(id: Long): NovelChapter? {
        return novelChapterDao.getById(id)?.toDomain()
    }

    override suspend fun insertChapter(chapter: NovelChapter): Long {
        return novelChapterDao.insert(chapter.toEntity())
    }

    override suspend fun updateChapter(chapter: NovelChapter) {
        novelChapterDao.update(chapter.toEntity().copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteChapter(chapter: NovelChapter) {
        novelChapterDao.delete(chapter.toEntity())
    }

    override suspend fun deleteChaptersByNovelId(novelId: Long) {
        novelChapterDao.deleteByNovelId(novelId)
    }

    // ========== 角色关联 ==========

    override fun getCharacterIdsByNovelId(novelId: Long): Flow<List<Long>> {
        return novelCharacterDao.getByNovelId(novelId).map { list -> list.map { it.characterId } }
    }

    override suspend fun getCharacterIdsByNovelIdSync(novelId: Long): List<Long> {
        return novelCharacterDao.getByNovelIdSync(novelId).map { it.characterId }
    }

    override suspend fun setNovelCharacters(novelId: Long, characterIds: List<Long>) {
        novelCharacterDao.deleteByNovelId(novelId)
        if (characterIds.isNotEmpty()) {
            novelCharacterDao.insertAll(
                characterIds.map { NovelCharacterEntity(novelId, it) }
            )
        }
    }

    // ========== 映射 ==========

    private fun NovelEntity.toDomain() = Novel(
        id = id,
        title = title,
        description = description,
        worldBookId = worldBookId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Novel.toEntity() = NovelEntity(
        id = id,
        title = title,
        description = description,
        worldBookId = worldBookId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun NovelChapterEntity.toDomain() = NovelChapter(
        id = id,
        novelId = novelId,
        chapterNumber = chapterNumber,
        title = title,
        outline = outline,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun NovelChapter.toEntity() = NovelChapterEntity(
        id = id,
        novelId = novelId,
        chapterNumber = chapterNumber,
        title = title,
        outline = outline,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
