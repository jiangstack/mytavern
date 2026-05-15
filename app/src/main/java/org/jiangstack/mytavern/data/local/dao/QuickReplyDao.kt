package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.QuickReplyEntity

@Dao
interface QuickReplyDao {
    @Query("SELECT * FROM quick_replies ORDER BY id DESC")
    fun getAll(): Flow<List<QuickReplyEntity>>

    @Query("SELECT * FROM quick_replies WHERE id = :id")
    suspend fun getById(id: Long): QuickReplyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quickReply: QuickReplyEntity): Long

    @Update
    suspend fun update(quickReply: QuickReplyEntity)

    @Delete
    suspend fun delete(quickReply: QuickReplyEntity)
}
