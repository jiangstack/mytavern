package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.TownSnapshotEntity

@Dao
interface TownSnapshotDao {
    @Query("SELECT * FROM town_snapshots WHERE townId = :townId ORDER BY createdAt DESC")
    fun getByTownId(townId: Long): Flow<List<TownSnapshotEntity>>

    @Query("SELECT * FROM town_snapshots WHERE id = :id")
    suspend fun getById(id: Long): TownSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: TownSnapshotEntity): Long

    @Update
    suspend fun update(snapshot: TownSnapshotEntity)

    @Delete
    suspend fun delete(snapshot: TownSnapshotEntity)

    @Query("DELETE FROM town_snapshots WHERE townId = :townId")
    suspend fun deleteByTownId(townId: Long)
}
