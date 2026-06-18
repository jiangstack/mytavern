package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.InteractiveGameStateEntity

@Dao
interface InteractiveGameStateDao {
    @Query("SELECT * FROM interactive_game_states WHERE gameId = :gameId")
    fun getByGameId(gameId: Long): Flow<InteractiveGameStateEntity?>

    @Query("SELECT * FROM interactive_game_states WHERE gameId = :gameId")
    suspend fun getByGameIdSync(gameId: Long): InteractiveGameStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: InteractiveGameStateEntity)
}
