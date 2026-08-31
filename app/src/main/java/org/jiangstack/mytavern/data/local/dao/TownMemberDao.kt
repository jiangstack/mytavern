package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.TownMemberEntity

@Dao
interface TownMemberDao {
    @Query("SELECT * FROM town_members WHERE townId = :townId ORDER BY id ASC")
    fun getByTownId(townId: Long): Flow<List<TownMemberEntity>>

    @Query("SELECT * FROM town_members WHERE townId = :townId ORDER BY id ASC")
    suspend fun getByTownIdSync(townId: Long): List<TownMemberEntity>

    @Query("SELECT * FROM town_members WHERE id = :id")
    suspend fun getById(id: Long): TownMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: TownMemberEntity): Long

    @Update
    suspend fun update(member: TownMemberEntity)

    @Delete
    suspend fun delete(member: TownMemberEntity)

    @Query("DELETE FROM town_members WHERE townId = :townId")
    suspend fun deleteByTownId(townId: Long)
}
