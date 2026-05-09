package org.jiangstack.mytavern.domain.repository

import kotlinx.coroutines.flow.Flow
import org.jiangstack.mytavern.domain.model.Character

interface CharacterRepository {
    fun getAllCharacters(): Flow<List<Character>>
    suspend fun getCharacterById(id: Long): Character?
    suspend fun insertCharacter(character: Character): Long
    suspend fun updateCharacter(character: Character)
    suspend fun deleteCharacter(character: Character)
}
