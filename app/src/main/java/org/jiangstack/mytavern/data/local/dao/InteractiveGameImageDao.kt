package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.InteractiveGameImageEntity

@Dao
interface InteractiveGameImageDao {
    @Query("SELECT * FROM interactive_game_images WHERE gameId = :gameId ORDER BY id DESC")
    fun getByGameId(gameId: Long): Flow<List<InteractiveGameImageEntity>>

    @Query("SELECT * FROM interactive_game_images WHERE id = :id")
    suspend fun getById(id: Long): InteractiveGameImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: InteractiveGameImageEntity): Long

    @Delete
    suspend fun delete(image: InteractiveGameImageEntity)
}
