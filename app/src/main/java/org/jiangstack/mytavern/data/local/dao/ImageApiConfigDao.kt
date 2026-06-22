package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.ImageApiConfigEntity

@Dao
interface ImageApiConfigDao {
    @Query("SELECT * FROM image_api_configs ORDER BY id DESC")
    fun getAll(): Flow<List<ImageApiConfigEntity>>

    @Query("SELECT * FROM image_api_configs WHERE id = :id")
    suspend fun getById(id: Long): ImageApiConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: ImageApiConfigEntity): Long

    @Update
    suspend fun update(config: ImageApiConfigEntity)

    @Delete
    suspend fun delete(config: ImageApiConfigEntity)
}
