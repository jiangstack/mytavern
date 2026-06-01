package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.NovelChapterEntity

@Dao
interface NovelChapterDao {
    @Query("SELECT * FROM novel_chapters WHERE novelId = :novelId ORDER BY chapterNumber ASC")
    fun getByNovelId(novelId: Long): Flow<List<NovelChapterEntity>>

    @Query("SELECT * FROM novel_chapters WHERE novelId = :novelId ORDER BY chapterNumber ASC")
    suspend fun getByNovelIdSync(novelId: Long): List<NovelChapterEntity>

    @Query("SELECT * FROM novel_chapters WHERE id = :id")
    suspend fun getById(id: Long): NovelChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chapter: NovelChapterEntity): Long

    @Update
    suspend fun update(chapter: NovelChapterEntity)

    @Delete
    suspend fun delete(chapter: NovelChapterEntity)

    @Query("DELETE FROM novel_chapters WHERE novelId = :novelId")
    suspend fun deleteByNovelId(novelId: Long)
}
