package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.TownLogEntity

@Dao
interface TownLogDao {
    @Query("SELECT * FROM town_logs WHERE townId = :townId ORDER BY id DESC")
    fun getByTownId(townId: Long): Flow<List<TownLogEntity>>

    @Query("SELECT * FROM town_logs WHERE townId = :townId ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentSync(townId: Long, limit: Int): List<TownLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: TownLogEntity): Long

    @Query("DELETE FROM town_logs WHERE townId = :townId")
    suspend fun deleteByTownId(townId: Long)
}
