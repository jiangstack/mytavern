package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.NovelCharacterItemEntity

@Dao
interface NovelCharacterItemDao {
    @Query("SELECT * FROM novel_character_items WHERE novelId = :novelId ORDER BY id ASC")
    fun getByNovelId(novelId: Long): Flow<List<NovelCharacterItemEntity>>

    @Query("SELECT * FROM novel_character_items WHERE novelId = :novelId ORDER BY id ASC")
    suspend fun getByNovelIdSync(novelId: Long): List<NovelCharacterItemEntity>

    @Query("SELECT * FROM novel_character_items WHERE novelId = :novelId AND characterId = :characterId ORDER BY id ASC")
    suspend fun getByNovelAndCharacterIdSync(novelId: Long, characterId: Long): List<NovelCharacterItemEntity>

    @Query("SELECT * FROM novel_character_items WHERE id = :id")
    suspend fun getById(id: Long): NovelCharacterItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: NovelCharacterItemEntity): Long

    @Update
    suspend fun update(item: NovelCharacterItemEntity)

    @Delete
    suspend fun delete(item: NovelCharacterItemEntity)

    @Query("DELETE FROM novel_character_items WHERE novelId = :novelId")
    suspend fun deleteByNovelId(novelId: Long)
}
