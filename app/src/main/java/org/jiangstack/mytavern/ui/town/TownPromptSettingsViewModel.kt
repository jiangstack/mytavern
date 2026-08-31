package org.jiangstack.mytavern.ui.town

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.PromptBlockConfig
import org.jiangstack.mytavern.domain.model.PromptBlockDefaults
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository

class TownPromptSettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val blocks: StateFlow<List<PromptBlockConfig>> =
        userPreferencesRepository.townPromptBlocks
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                PromptBlockDefaults.townBlocks()
            )

    val maxIterations: StateFlow<Int> =
        userPreferencesRepository.townMaxIterations
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2)

    fun toggleBlock(index: Int) {
        viewModelScope.launch {
            val current = blocks.value.toMutableList()
            if (index in current.indices) {
                current[index] = current[index].copy(isEnabled = !current[index].isEnabled)
                userPreferencesRepository.setTownPromptBlocks(current)
            }
        }
    }

    fun moveBlockUp(index: Int) {
        if (index <= 0) return
        viewModelScope.launch {
            val current = blocks.value.toMutableList()
            val temp = current[index]
            current[index] = current[index - 1]
            current[index - 1] = temp
            val updated = current.mapIndexed { i, block -> block.copy(sortOrder = i) }
            userPreferencesRepository.setTownPromptBlocks(updated)
        }
    }

    fun moveBlockDown(index: Int) {
        val currentList = blocks.value
        if (index < 0 || index >= currentList.size - 1) return
        viewModelScope.launch {
            val current = currentList.toMutableList()
            val temp = current[index]
            current[index] = current[index + 1]
            current[index + 1] = temp
            val updated = current.mapIndexed { i, block -> block.copy(sortOrder = i) }
            userPreferencesRepository.setTownPromptBlocks(updated)
        }
    }

    fun updateBlockContent(index: Int, content: String) {
        viewModelScope.launch {
            val current = blocks.value.toMutableList()
            if (index in current.indices) {
                current[index] = current[index].copy(
                    customContent = content.trim().takeIf { it.isNotEmpty() }
                )
                userPreferencesRepository.setTownPromptBlocks(current)
            }
        }
    }

    fun resetBlocks() {
        viewModelScope.launch {
            userPreferencesRepository.setTownPromptBlocks(PromptBlockDefaults.townBlocks())
        }
    }

    fun setMaxIterations(value: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setTownMaxIterations(value)
        }
    }

    companion object {
        fun factory(
            userPreferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TownPromptSettingsViewModel(userPreferencesRepository) as T
                }
            }
        }
    }
}
