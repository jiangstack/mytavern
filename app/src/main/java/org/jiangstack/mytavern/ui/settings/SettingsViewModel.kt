package org.jiangstack.mytavern.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.CharacterType
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.LlmConfigRepository
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository

class SettingsViewModel(
    llmConfigRepository: LlmConfigRepository,
    private val characterRepository: CharacterRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val configs = llmConfigRepository.getAllConfigs()

    val defaultUserCharacter = combine(
        userPreferencesRepository.defaultUserCharacterId,
        characterRepository.getUserCharacters()
    ) { defaultId, userChars ->
        userChars.find { it.id == defaultId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userCharacters = characterRepository.getUserCharacters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDefaultUserCharacter(id: Long?) {
        viewModelScope.launch {
            userPreferencesRepository.setDefaultUserCharacterId(id)
        }
    }

    companion object {
        fun factory(
            llmConfigRepository: LlmConfigRepository,
            characterRepository: CharacterRepository,
            userPreferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        llmConfigRepository,
                        characterRepository,
                        userPreferencesRepository
                    ) as T
                }
            }
        }
    }
}
