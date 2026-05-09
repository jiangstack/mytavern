package org.jiangstack.mytavern.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jiangstack.mytavern.data.local.dao.CharacterDao
import org.jiangstack.mytavern.data.local.entity.CharacterEntity
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.repository.CharacterRepository

class CharacterRepositoryImpl(
    private val characterDao: CharacterDao
) : CharacterRepository {

    override fun getAllCharacters(): Flow<List<Character>> {
        return characterDao.getAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getCharacterById(id: Long): Character? {
        return characterDao.getById(id)?.toDomain()
    }

    override suspend fun insertCharacter(character: Character): Long {
        return characterDao.insert(character.toEntity())
    }

    override suspend fun updateCharacter(character: Character) {
        characterDao.update(character.toEntity())
    }

    override suspend fun deleteCharacter(character: Character) {
        characterDao.delete(character.toEntity())
    }

    private fun CharacterEntity.toDomain() = Character(
        id = id,
        name = name,
        description = description,
        avatarUri = avatarUri
    )

    private fun Character.toEntity() = CharacterEntity(
        id = id,
        name = name,
        description = description,
        avatarUri = avatarUri
    )
}
