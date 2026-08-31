package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.TownLocationEntity

@Dao
interface TownLocationDao {
    @Query("SELECT * FROM town_locations WHERE townId = :townId ORDER BY id ASC")
    fun getByTownId(townId: Long): Flow<List<TownLocationEntity>>

    @Query("SELECT * FROM town_locations WHERE townId = :townId ORDER BY id ASC")
    suspend fun getByTownIdSync(townId: Long): List<TownLocationEntity>

    @Query("SELECT * FROM town_locations WHERE id = :id")
    suspend fun getById(id: Long): TownLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: TownLocationEntity): Long

    @Update
    suspend fun update(location: TownLocationEntity)

    @Delete
    suspend fun delete(location: TownLocationEntity)

    @Query("DELETE FROM town_locations WHERE townId = :townId")
    suspend fun deleteByTownId(townId: Long)
}
