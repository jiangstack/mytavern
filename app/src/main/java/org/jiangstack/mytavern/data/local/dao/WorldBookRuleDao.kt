package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.WorldBookRuleEntity

@Dao
interface WorldBookRuleDao {
    @Query("SELECT * FROM world_book_rules WHERE worldBookId = :worldBookId ORDER BY id ASC")
    fun getByWorldBookId(worldBookId: Long): Flow<List<WorldBookRuleEntity>>

    @Query("SELECT * FROM world_book_rules WHERE id = :id")
    suspend fun getById(id: Long): WorldBookRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: WorldBookRuleEntity): Long

    @Update
    suspend fun update(rule: WorldBookRuleEntity)

    @Delete
    suspend fun delete(rule: WorldBookRuleEntity)
}
