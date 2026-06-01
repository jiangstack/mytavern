package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.NovelCharacterEntity

@Dao
interface NovelCharacterDao {
    @Query("SELECT * FROM novel_characters WHERE novelId = :novelId")
    fun getByNovelId(novelId: Long): Flow<List<NovelCharacterEntity>>

    @Query("SELECT * FROM novel_characters WHERE novelId = :novelId")
    suspend fun getByNovelIdSync(novelId: Long): List<NovelCharacterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: NovelCharacterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<NovelCharacterEntity>)

    @Query("DELETE FROM novel_characters WHERE novelId = :novelId")
    suspend fun deleteByNovelId(novelId: Long)
}
