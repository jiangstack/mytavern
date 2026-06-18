package org.jiangstack.mytavern.domain.repository

import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.domain.model.InteractiveGame
import org.jiangstack.mytavern.domain.model.InteractiveGameState
import org.jiangstack.mytavern.domain.model.InteractiveMessage

interface InteractiveGameRepository {
    // 游戏 CRUD
    fun getAllGames(): Flow<List<InteractiveGame>>
    suspend fun getGameById(id: Long): InteractiveGame?
    suspend fun insertGame(game: InteractiveGame): Long
    suspend fun updateGame(game: InteractiveGame)
    suspend fun deleteGame(game: InteractiveGame)

    // 消息 CRUD
    fun getMessagesByGameId(gameId: Long): Flow<List<InteractiveMessage>>
    suspend fun getLatestMessagesByGameId(gameId: Long, limit: Int): List<InteractiveMessage>
    suspend fun insertMessage(message: InteractiveMessage): Long
    suspend fun deleteMessagesByGameId(gameId: Long)

    // 游戏状态
    fun getGameStateByGameId(gameId: Long): Flow<InteractiveGameState?>
    suspend fun getGameStateByGameIdSync(gameId: Long): InteractiveGameState?
    suspend fun insertOrUpdateGameState(state: InteractiveGameState)

    // 角色关联
    fun getCharacterIdsByGameId(gameId: Long): Flow<List<Long>>
    suspend fun getCharacterIdsByGameIdSync(gameId: Long): List<Long>
    suspend fun setGameCharacters(gameId: Long, characterIds: List<Long>)
}
