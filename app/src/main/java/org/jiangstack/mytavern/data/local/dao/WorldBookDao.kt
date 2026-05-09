package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.WorldBookEntity

@Dao
interface WorldBookDao {
    @Query("SELECT * FROM world_books ORDER BY id DESC")
    fun getAll(): Flow<List<WorldBookEntity>>

    @Query("SELECT * FROM world_books WHERE id = :id")
    suspend fun getById(id: Long): WorldBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(worldBook: WorldBookEntity): Long

    @Update
    suspend fun update(worldBook: WorldBookEntity)

    @Delete
    suspend fun delete(worldBook: WorldBookEntity)
}
