package org.jiangstack.mytavern.domain.repository

import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.domain.model.Novel
import org.jiangstack.mytavern.domain.model.NovelChapter

interface NovelRepository {
    // 小说 CRUD
    fun getAllNovels(): Flow<List<Novel>>
    suspend fun getNovelById(id: Long): Novel?
    suspend fun insertNovel(novel: Novel): Long
    suspend fun updateNovel(novel: Novel)
    suspend fun deleteNovel(novel: Novel)

    // 章节 CRUD
    fun getChaptersByNovelId(novelId: Long): Flow<List<NovelChapter>>
    suspend fun getChaptersByNovelIdSync(novelId: Long): List<NovelChapter>
    suspend fun getChapterById(id: Long): NovelChapter?
    suspend fun insertChapter(chapter: NovelChapter): Long
    suspend fun updateChapter(chapter: NovelChapter)
    suspend fun deleteChapter(chapter: NovelChapter)
    suspend fun deleteChaptersByNovelId(novelId: Long)

    // 角色关联
    fun getCharacterIdsByNovelId(novelId: Long): Flow<List<Long>>
    suspend fun getCharacterIdsByNovelIdSync(novelId: Long): List<Long>
    suspend fun setNovelCharacters(novelId: Long, characterIds: List<Long>)
}
