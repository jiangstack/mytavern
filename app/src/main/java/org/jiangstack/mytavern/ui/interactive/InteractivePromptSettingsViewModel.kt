package org.jiangstack.mytavern.ui.interactive

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

class InteractivePromptSettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val blocks: StateFlow<List<PromptBlockConfig>> =
        userPreferencesRepository.interactivePromptBlocks
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                PromptBlockDefaults.interactiveStoryBlocks()
            )

    fun toggleBlock(index: Int) {
        viewModelScope.launch {
            val current = blocks.value.toMutableList()
            if (index in current.indices) {
                current[index] = current[index].copy(isEnabled = !current[index].isEnabled)
                userPreferencesRepository.setInteractivePromptBlocks(current)
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
            userPreferencesRepository.setInteractivePromptBlocks(updated)
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
            userPreferencesRepository.setInteractivePromptBlocks(updated)
        }
    }

    fun updateBlockContent(index: Int, content: String) {
        viewModelScope.launch {
            val current = blocks.value.toMutableList()
            if (index in current.indices) {
                current[index] = current[index].copy(
                    customContent = content.trim().takeIf { it.isNotEmpty() }
                )
                userPreferencesRepository.setInteractivePromptBlocks(current)
            }
        }
    }

    fun resetBlocks() {
        viewModelScope.launch {
            userPreferencesRepository.setInteractivePromptBlocks(PromptBlockDefaults.interactiveStoryBlocks())
        }
    }

    companion object {
        fun factory(
            userPreferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return InteractivePromptSettingsViewModel(userPreferencesRepository) as T
                }
            }
        }
    }
}
