package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.ChatSessionEntity

@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getById(id: Long): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ChatSessionEntity): Long

    @Update
    suspend fun update(session: ChatSessionEntity)

    @Delete
    suspend fun delete(session: ChatSessionEntity)
}
