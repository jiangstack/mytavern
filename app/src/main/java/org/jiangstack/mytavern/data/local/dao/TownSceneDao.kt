package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.TownSceneEntity

@Dao
interface TownSceneDao {
    @Query("SELECT * FROM town_scenes WHERE townId = :townId ORDER BY day DESC, hour DESC, id DESC")
    fun getByTownId(townId: Long): Flow<List<TownSceneEntity>>

    @Query("SELECT * FROM town_scenes WHERE id = :id")
    suspend fun getById(id: Long): TownSceneEntity?

    @Query("SELECT * FROM town_scenes WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<TownSceneEntity?>

    @Query(
        "SELECT * FROM town_scenes WHERE townId = :townId AND status = 'PENDING' " +
            "ORDER BY id ASC LIMIT 1"
    )
    suspend fun getFirstPendingSync(townId: Long): TownSceneEntity?

    @Query(
        "SELECT * FROM town_scenes WHERE townId = :townId AND status IN ('INTERACTIVE', 'AWAITING_PLAYER') " +
            "ORDER BY id DESC LIMIT 1"
    )
    suspend fun getActiveInteractiveSync(townId: Long): TownSceneEntity?

    @Query("SELECT * FROM town_scenes WHERE townId = :townId ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentSync(townId: Long, limit: Int): List<TownSceneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scene: TownSceneEntity): Long

    @Update
    suspend fun update(scene: TownSceneEntity)

    @Delete
    suspend fun delete(scene: TownSceneEntity)

    @Query("DELETE FROM town_scenes WHERE townId = :townId")
    suspend fun deleteByTownId(townId: Long)
}
