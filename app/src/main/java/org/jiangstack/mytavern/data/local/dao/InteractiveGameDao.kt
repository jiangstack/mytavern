package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.InteractiveGameEntity

@Dao
interface InteractiveGameDao {
    @Query("SELECT * FROM interactive_games ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<InteractiveGameEntity>>

    @Query("SELECT * FROM interactive_games WHERE id = :id")
    suspend fun getById(id: Long): InteractiveGameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: InteractiveGameEntity): Long

    @Update
    suspend fun update(game: InteractiveGameEntity)

    @Delete
    suspend fun delete(game: InteractiveGameEntity)
}
