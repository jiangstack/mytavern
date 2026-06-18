package org.jiangstack.mytavern.domain.repository

import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.CharacterType

interface CharacterRepository {
    fun getAllCharacters(): Flow<List<Character>>
    fun getCharactersByType(type: CharacterType): Flow<List<Character>>
    fun getUserCharacters(): Flow<List<Character>>
    fun getAiCharacters(): Flow<List<Character>>
    suspend fun getCharacterById(id: Long): Character?
    suspend fun getCharactersByIds(ids: List<Long>): List<Character>
    suspend fun insertCharacter(character: Character): Long
    suspend fun updateCharacter(character: Character)
    suspend fun deleteCharacter(character: Character)
}
