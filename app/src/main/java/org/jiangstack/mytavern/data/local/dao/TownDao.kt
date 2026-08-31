package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.TownEntity

@Dao
interface TownDao {
    @Query("SELECT * FROM towns ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<TownEntity>>

    @Query("SELECT * FROM towns WHERE id = :id")
    suspend fun getById(id: Long): TownEntity?

    @Query("SELECT * FROM towns WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<TownEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(town: TownEntity): Long

    @Update
    suspend fun update(town: TownEntity)

    @Delete
    suspend fun delete(town: TownEntity)
}
