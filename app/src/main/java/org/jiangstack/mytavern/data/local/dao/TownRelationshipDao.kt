package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.TownRelationshipEntity

@Dao
interface TownRelationshipDao {
    @Query("SELECT * FROM town_relationships WHERE townId = :townId")
    fun getByTownId(townId: Long): Flow<List<TownRelationshipEntity>>

    @Query("SELECT * FROM town_relationships WHERE townId = :townId")
    suspend fun getByTownIdSync(townId: Long): List<TownRelationshipEntity>

    @Query(
        "SELECT * FROM town_relationships WHERE townId = :townId AND " +
            "((memberAId = :a AND memberBId = :b) OR (memberAId = :b AND memberBId = :a)) LIMIT 1"
    )
    suspend fun findBetween(townId: Long, a: Long, b: Long): TownRelationshipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(relationship: TownRelationshipEntity): Long

    @Update
    suspend fun update(relationship: TownRelationshipEntity)

    @Delete
    suspend fun delete(relationship: TownRelationshipEntity)

    @Query("DELETE FROM town_relationships WHERE townId = :townId")
    suspend fun deleteByTownId(townId: Long)
}
