package org.jiangstack.mytavern.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.CharacterType
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository

class CharacterListViewModel(
    private val characterRepository: CharacterRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val selectedType = MutableStateFlow<CharacterType?>(null)

    val characters = combine(
        characterRepository.getAllCharacters(),
        selectedType
    ) { list, type ->
        if (type == null) list else list.filter { it.type == type }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultUserCharacterId = userPreferencesRepository.defaultUserCharacterId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setFilterType(type: CharacterType?) {
        selectedType.value = type
    }

    fun setDefaultUserCharacter(id: Long) {
        viewModelScope.launch {
            userPreferencesRepository.setDefaultUserCharacterId(id)
        }
    }

    fun saveCharacter(character: Character) {
        viewModelScope.launch {
            if (character.id == 0L) {
                characterRepository.insertCharacter(character)
            } else {
                characterRepository.updateCharacter(character)
            }
        }
    }

    fun deleteCharacter(character: Character) {
        viewModelScope.launch {
            characterRepository.deleteCharacter(character)
        }
    }

    companion object {
        fun factory(
            characterRepository: CharacterRepository,
            userPreferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CharacterListViewModel(characterRepository, userPreferencesRepository) as T
                }
            }
        }
    }
}
