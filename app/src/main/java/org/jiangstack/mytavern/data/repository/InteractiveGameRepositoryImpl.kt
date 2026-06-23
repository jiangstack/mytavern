package org.jiangstack.mytavern.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jiangstack.mytavern.data.local.dao.InteractiveCheckpointDao
import org.jiangstack.mytavern.data.local.dao.InteractiveGameCharacterDao
import org.jiangstack.mytavern.data.local.dao.InteractiveGameDao
import org.jiangstack.mytavern.data.local.dao.InteractiveGameStateDao
import org.jiangstack.mytavern.data.local.dao.InteractiveMessageDao
import org.jiangstack.mytavern.data.local.entity.InteractiveCheckpointEntity
import org.jiangstack.mytavern.data.local.entity.InteractiveGameCharacterEntity
import org.jiangstack.mytavern.data.local.entity.InteractiveGameEntity
import org.jiangstack.mytavern.data.local.entity.InteractiveGameStateEntity
import org.jiangstack.mytavern.data.local.entity.InteractiveMessageEntity
import org.jiangstack.mytavern.domain.model.InteractiveCheckpoint
import org.jiangstack.mytavern.domain.model.InteractiveCheckpointSnapshot
import org.jiangstack.mytavern.domain.model.InteractiveGame
import org.jiangstack.mytavern.domain.model.InteractiveGameState
import org.jiangstack.mytavern.domain.model.InteractiveMessage
import org.jiangstack.mytavern.domain.repository.InteractiveGameRepository

class InteractiveGameRepositoryImpl(
    private val gameDao: InteractiveGameDao,
    private val gameCharacterDao: InteractiveGameCharacterDao,
    private val messageDao: InteractiveMessageDao,
    private val gameStateDao: InteractiveGameStateDao,
    private val checkpointDao: InteractiveCheckpointDao,
    private val json: Json
) : InteractiveGameRepository {

    // ========== 游戏 ==========

    override fun getAllGames(): Flow<List<InteractiveGame>> {
        return gameDao.getAll().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getGameById(id: Long): InteractiveGame? {
        val entity = gameDao.getById(id) ?: return null
        val characterIds = getCharacterIdsByGameIdSync(id)
        return entity.toDomain().copy(characterIds = characterIds)
    }

    override suspend fun insertGame(game: InteractiveGame): Long {
        val id = gameDao.insert(game.toEntity())
        if (game.characterIds.isNotEmpty()) {
            gameCharacterDao.insertAll(
                game.characterIds.map { InteractiveGameCharacterEntity(id, it) }
            )
        }
        return id
    }

    override suspend fun updateGame(game: InteractiveGame) {
        gameDao.update(game.toEntity().copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteGame(game: InteractiveGame) {
        gameDao.delete(game.toEntity())
    }

    // ========== 消息 ==========

    override fun getMessagesByGameId(gameId: Long): Flow<List<InteractiveMessage>> {
        return messageDao.getByGameId(gameId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getLatestMessagesByGameId(gameId: Long, limit: Int): List<InteractiveMessage> {
        return messageDao.getLatestByGameId(gameId, limit).map { it.toDomain() }
    }

    override suspend fun insertMessage(message: InteractiveMessage): Long {
        return messageDao.insert(message.toEntity())
    }

    override suspend fun deleteMessagesByGameId(gameId: Long) {
        messageDao.deleteByGameId(gameId)
    }

    // ========== 游戏状态 ==========

    override fun getGameStateByGameId(gameId: Long): Flow<InteractiveGameState?> {
        return gameStateDao.getByGameId(gameId).map { it?.toDomain() }
    }

    override suspend fun getGameStateByGameIdSync(gameId: Long): InteractiveGameState? {
        return gameStateDao.getByGameIdSync(gameId)?.toDomain()
    }

    override suspend fun insertOrUpdateGameState(state: InteractiveGameState) {
        gameStateDao.insertOrUpdate(state.toEntity())
    }

    // ========== 角色关联 ==========

    override fun getCharacterIdsByGameId(gameId: Long): Flow<List<Long>> {
        return gameCharacterDao.getByGameId(gameId).map { list -> list.map { it.characterId } }
    }

    override suspend fun getCharacterIdsByGameIdSync(gameId: Long): List<Long> {
        return gameCharacterDao.getByGameIdSync(gameId).map { it.characterId }
    }

    override suspend fun setGameCharacters(gameId: Long, characterIds: List<Long>) {
        gameCharacterDao.deleteByGameId(gameId)
        if (characterIds.isNotEmpty()) {
            gameCharacterDao.insertAll(
                characterIds.map { InteractiveGameCharacterEntity(gameId, it) }
            )
        }
    }

    // ========== 保存点 ==========

    override fun getCheckpointsByGameId(gameId: Long): Flow<List<InteractiveCheckpoint>> {
        return checkpointDao.getByGameId(gameId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getCheckpointById(id: Long): InteractiveCheckpoint? {
        return checkpointDao.getById(id)?.toDomain()
    }

    override suspend fun createCheckpoint(
        gameId: Long,
        parentId: Long?,
        name: String,
        snapshot: InteractiveCheckpointSnapshot
    ): Long {
        val entity = InteractiveCheckpointEntity(
            gameId = gameId,
            parentId = parentId,
            name = name,
            snapshot = json.encodeToString(snapshot)
        )
        return checkpointDao.insert(entity)
    }

    override suspend fun updateCheckpointName(id: Long, name: String) {
        checkpointDao.getById(id)?.let {
            checkpointDao.update(it.copy(name = name))
        }
    }

    override suspend fun deleteCheckpoint(checkpoint: InteractiveCheckpoint) {
        val allCheckpoints = checkpointDao.getByGameId(checkpoint.gameId).first()
        val subtreeIds = buildSubtreeIds(allCheckpoints, checkpoint.id)

        checkpointDao.delete(checkpoint.toEntity())

        val currentState = gameStateDao.getByGameIdSync(checkpoint.gameId)
        if (currentState != null && currentState.activeCheckpointId in subtreeIds) {
            val newActiveId = checkpoint.parentId
            gameStateDao.insertOrUpdate(
                currentState.copy(activeCheckpointId = newActiveId)
            )
        }
    }

    override suspend fun clearCheckpointsByGameId(gameId: Long) {
        checkpointDao.deleteByGameId(gameId)
        val currentState = gameStateDao.getByGameIdSync(gameId)
        if (currentState != null && currentState.activeCheckpointId != null) {
            gameStateDao.insertOrUpdate(
                currentState.copy(activeCheckpointId = null)
            )
        }
    }

    override suspend fun loadCheckpoint(checkpointId: Long) {
        val checkpoint = checkpointDao.getById(checkpointId) ?: return
        val snapshot = checkpoint.toDomain().snapshot

        messageDao.deleteByGameId(checkpoint.gameId)

        val restoredMessages = snapshot.messages.map { it.copy(id = 0) }
        if (restoredMessages.isNotEmpty()) {
            messageDao.insertAll(restoredMessages.map { it.toEntity() })
        }

        val restoredState = snapshot.gameState?.copy(
            gameId = checkpoint.gameId,
            activeCheckpointId = checkpoint.id
        ) ?: InteractiveGameState(
            gameId = checkpoint.gameId,
            activeCheckpointId = checkpoint.id
        )
        gameStateDao.insertOrUpdate(restoredState.toEntity())
    }

    // ========== 映射 ==========

    private fun InteractiveGameEntity.toDomain() = InteractiveGame(
        id = id,
        title = title,
        narratorStyle = narratorStyle,
        storyBackground = storyBackground,
        storyMainPlot = storyMainPlot,
        windowWordCount = windowWordCount,
        playCharacterId = playCharacterId,
        worldBookId = worldBookId,
        backgroundImageUri = backgroundImageUri,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun InteractiveGame.toEntity() = InteractiveGameEntity(
        id = id,
        title = title,
        narratorStyle = narratorStyle,
        storyBackground = storyBackground,
        storyMainPlot = storyMainPlot,
        windowWordCount = windowWordCount,
        playCharacterId = playCharacterId,
        worldBookId = worldBookId,
        backgroundImageUri = backgroundImageUri,
        createdAt = createdAt,
        updatedAt = updatedAt
    )


    private fun InteractiveMessageEntity.toDomain() = InteractiveMessage(
        id = id,
        gameId = gameId,
        role = role,
        content = content,
        actionOptions = actionOptions?.let {
            try { json.decodeFromString<List<String>>(it) } catch (_: Exception) { emptyList() }
        } ?: emptyList(),
        timestamp = timestamp
    )

    private fun InteractiveMessage.toEntity() = InteractiveMessageEntity(
        id = id,
        gameId = gameId,
        role = role,
        content = content,
        actionOptions = if (actionOptions.isNotEmpty()) json.encodeToString(actionOptions) else null,
        timestamp = timestamp
    )

    private fun InteractiveGameStateEntity.toDomain() = InteractiveGameState(
        gameId = gameId,
        environment = environment,
        characterStatus = characterStatus,
        characterItems = characterItems,
        activeCheckpointId = activeCheckpointId
    )

    private fun InteractiveGameState.toEntity() = InteractiveGameStateEntity(
        gameId = gameId,
        environment = environment,
        characterStatus = characterStatus,
        characterItems = characterItems,
        activeCheckpointId = activeCheckpointId,
        updatedAt = System.currentTimeMillis()
    )

    private fun InteractiveCheckpointEntity.toDomain() = InteractiveCheckpoint(
        id = id,
        gameId = gameId,
        parentId = parentId,
        name = name,
        snapshot = try {
            json.decodeFromString<InteractiveCheckpointSnapshot>(snapshot)
        } catch (_: Exception) {
            InteractiveCheckpointSnapshot(emptyList(), null)
        },
        createdAt = createdAt
    )

    private fun InteractiveCheckpoint.toEntity() = InteractiveCheckpointEntity(
        id = id,
        gameId = gameId,
        parentId = parentId,
        name = name,
        snapshot = json.encodeToString(snapshot),
        createdAt = createdAt
    )

    private fun buildSubtreeIds(checkpoints: List<InteractiveCheckpointEntity>, rootId: Long): Set<Long> {
        val byParent = checkpoints.groupBy { it.parentId }
        val result = mutableSetOf<Long>()
        val queue = ArrayDeque<Long>()
        queue.add(rootId)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (result.add(current)) {
                byParent[current]?.forEach { queue.add(it.id) }
            }
        }
        return result
    }
}
