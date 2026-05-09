package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.LlmConfigEntity

@Dao
interface LlmConfigDao {
    @Query("SELECT * FROM llm_configs ORDER BY id DESC")
    fun getAll(): Flow<List<LlmConfigEntity>>

    @Query("SELECT * FROM llm_configs WHERE id = :id")
    suspend fun getById(id: Long): LlmConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: LlmConfigEntity): Long

    @Update
    suspend fun update(config: LlmConfigEntity)

    @Delete
    suspend fun delete(config: LlmConfigEntity)
}
