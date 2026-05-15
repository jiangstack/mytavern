package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.SessionCharacterEntity

@Dao
interface SessionCharacterDao {
    @Query("SELECT * FROM session_characters WHERE sessionId = :sessionId")
    fun getBySessionId(sessionId: Long): Flow<List<SessionCharacterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SessionCharacterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SessionCharacterEntity>)

    @Query("DELETE FROM session_characters WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long)
}
