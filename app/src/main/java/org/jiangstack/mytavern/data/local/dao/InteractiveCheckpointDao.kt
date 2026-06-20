package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.InteractiveCheckpointEntity

@Dao
interface InteractiveCheckpointDao {
    @Query("SELECT * FROM interactive_checkpoints WHERE gameId = :gameId ORDER BY createdAt ASC")
    fun getByGameId(gameId: Long): Flow<List<InteractiveCheckpointEntity>>

    @Query("SELECT * FROM interactive_checkpoints WHERE id = :id")
    suspend fun getById(id: Long): InteractiveCheckpointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(checkpoint: InteractiveCheckpointEntity): Long

    @Update
    suspend fun update(checkpoint: InteractiveCheckpointEntity)

    @Delete
    suspend fun delete(checkpoint: InteractiveCheckpointEntity)
}
