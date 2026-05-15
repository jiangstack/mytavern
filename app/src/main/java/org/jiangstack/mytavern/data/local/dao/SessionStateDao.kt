package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.SessionStateEntity

@Dao
interface SessionStateDao {
    @Query("SELECT * FROM session_states WHERE sessionId = :sessionId ORDER BY updatedAt DESC")
    fun getBySessionId(sessionId: Long): Flow<List<SessionStateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: SessionStateEntity)

    @Query("DELETE FROM session_states WHERE sessionId = :sessionId AND stateKey = :stateKey")
    suspend fun delete(sessionId: Long, stateKey: String)

    @Query("DELETE FROM session_states WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long)
}
