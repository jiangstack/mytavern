package org.jiangstack.mytavern.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.data.local.entity.InteractiveGameCharacterEntity

@Dao
interface InteractiveGameCharacterDao {
    @Query("SELECT * FROM interactive_game_characters WHERE gameId = :gameId")
    fun getByGameId(gameId: Long): Flow<List<InteractiveGameCharacterEntity>>

    @Query("SELECT * FROM interactive_game_characters WHERE gameId = :gameId")
    suspend fun getByGameIdSync(gameId: Long): List<InteractiveGameCharacterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(characters: List<InteractiveGameCharacterEntity>)

    @Query("DELETE FROM interactive_game_characters WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: Long)
}
