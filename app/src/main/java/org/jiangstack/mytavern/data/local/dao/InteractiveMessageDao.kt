package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.InteractiveMessageEntity

@Dao
interface InteractiveMessageDao {
    @Query("SELECT * FROM interactive_messages WHERE gameId = :gameId ORDER BY timestamp ASC")
    fun getByGameId(gameId: Long): Flow<List<InteractiveMessageEntity>>

    @Query("SELECT * FROM interactive_messages WHERE gameId = :gameId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatestByGameId(gameId: Long, limit: Int): List<InteractiveMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: InteractiveMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<InteractiveMessageEntity>): List<Long>

    @Query("DELETE FROM interactive_messages WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: Long)
}
