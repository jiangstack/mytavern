package org.jiangstack.mytavern.ui.settings

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

class NovelPromptSettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val continueBlocks: StateFlow<List<PromptBlockConfig>> =
        userPreferencesRepository.novelPromptBlocks
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                PromptBlockDefaults.continueWritingBlocks()
            )

    val modifyBlocks: StateFlow<List<PromptBlockConfig>> =
        userPreferencesRepository.novelModifyPromptBlocks
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                PromptBlockDefaults.modifyBlocks()
            )

    val outlineBlocks: StateFlow<List<PromptBlockConfig>> =
        userPreferencesRepository.novelOutlinePromptBlocks
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                PromptBlockDefaults.outlineBlocks()
            )

    // region Continue blocks

    fun toggleContinueBlock(index: Int) {
        viewModelScope.launch {
            val current = continueBlocks.value.toMutableList()
            if (index in current.indices) {
                current[index] = current[index].copy(isEnabled = !current[index].isEnabled)
                userPreferencesRepository.setNovelPromptBlocks(current)
            }
        }
    }

    fun moveContinueBlockUp(index: Int) {
        if (index <= 0) return
        viewModelScope.launch {
            val current = continueBlocks.value.toMutableList()
            val temp = current[index]
            current[index] = current[index - 1]
            current[index - 1] = temp
            val updated = current.mapIndexed { i, block -> block.copy(sortOrder = i) }
            userPreferencesRepository.setNovelPromptBlocks(updated)
        }
    }

    fun moveContinueBlockDown(index: Int) {
        val currentList = continueBlocks.value
        if (index < 0 || index >= currentList.size - 1) return
        viewModelScope.launch {
            val current = currentList.toMutableList()
            val temp = current[index]
            current[index] = current[index + 1]
            current[index + 1] = temp
            val updated = current.mapIndexed { i, block -> block.copy(sortOrder = i) }
            userPreferencesRepository.setNovelPromptBlocks(updated)
        }
    }

    fun updateContinueBlockContent(index: Int, content: String) {
        viewModelScope.launch {
            val current = continueBlocks.value.toMutableList()
            if (index in current.indices) {
                current[index] = current[index].copy(
                    customContent = content.trim().takeIf { it.isNotEmpty() }
                )
                userPreferencesRepository.setNovelPromptBlocks(current)
            }
        }
    }

    fun resetContinueBlocks() {
        viewModelScope.launch {
            userPreferencesRepository.setNovelPromptBlocks(PromptBlockDefaults.continueWritingBlocks())
        }
    }

    // endregion

    // region Modify blocks

    fun toggleModifyBlock(index: Int) {
        viewModelScope.launch {
            val current = modifyBlocks.value.toMutableList()
            if (index in current.indices) {
                current[index] = current[index].copy(isEnabled = !current[index].isEnabled)
                userPreferencesRepository.setNovelModifyPromptBlocks(current)
            }
        }
    }

    fun moveModifyBlockUp(index: Int) {
        if (index <= 0) return
        viewModelScope.launch {
            val current = modifyBlocks.value.toMutableList()
            val temp = current[index]
            current[index] = current[index - 1]
            current[index - 1] = temp
            val updated = current.mapIndexed { i, block -> block.copy(sortOrder = i) }
            userPreferencesRepository.setNovelModifyPromptBlocks(updated)
        }
    }

    fun moveModifyBlockDown(index: Int) {
        val currentList = modifyBlocks.value
        if (index < 0 || index >= currentList.size - 1) return
        viewModelScope.launch {
            val current = currentList.toMutableList()
            val temp = current[index]
            current[index] = current[index + 1]
            current[index + 1] = temp
            val updated = current.mapIndexed { i, block -> block.copy(sortOrder = i) }
            userPreferencesRepository.setNovelModifyPromptBlocks(updated)
        }
    }

    fun updateModifyBlockContent(index: Int, content: String) {
        viewModelScope.launch {
            val current = modifyBlocks.value.toMutableList()
            if (index in current.indices) {
                current[index] = current[index].copy(
                    customContent = content.trim().takeIf { it.isNotEmpty() }
                )
                userPreferencesRepository.setNovelModifyPromptBlocks(current)
            }
        }
    }

    fun resetModifyBlocks() {
        viewModelScope.launch {
            userPreferencesRepository.setNovelModifyPromptBlocks(PromptBlockDefaults.modifyBlocks())
        }
    }

    // endregion

    // region Outline blocks

    fun toggleOutlineBlock(index: Int) {
        viewModelScope.launch {
            val current = outlineBlocks.value.toMutableList()
            if (index in current.indices) {
                current[index] = current[index].copy(isEnabled = !current[index].isEnabled)
                userPreferencesRepository.setNovelOutlinePromptBlocks(current)
            }
        }
    }

    fun moveOutlineBlockUp(index: Int) {
        if (index <= 0) return
        viewModelScope.launch {
            val current = outlineBlocks.value.toMutableList()
            val temp = current[index]
            current[index] = current[index - 1]
            current[index - 1] = temp
            val updated = current.mapIndexed { i, block -> block.copy(sortOrder = i) }
            userPreferencesRepository.setNovelOutlinePromptBlocks(updated)
        }
    }

    fun moveOutlineBlockDown(index: Int) {
        val currentList = outlineBlocks.value
        if (index < 0 || index >= currentList.size - 1) return
        viewModelScope.launch {
            val current = currentList.toMutableList()
            val temp = current[index]
            current[index] = current[index + 1]
            current[index + 1] = temp
            val updated = current.mapIndexed { i, block -> block.copy(sortOrder = i) }
            userPreferencesRepository.setNovelOutlinePromptBlocks(updated)
        }
    }

    fun updateOutlineBlockContent(index: Int, content: String) {
        viewModelScope.launch {
            val current = outlineBlocks.value.toMutableList()
            if (index in current.indices) {
                current[index] = current[index].copy(
                    customContent = content.trim().takeIf { it.isNotEmpty() }
                )
                userPreferencesRepository.setNovelOutlinePromptBlocks(current)
            }
        }
    }

    fun resetOutlineBlocks() {
        viewModelScope.launch {
            userPreferencesRepository.setNovelOutlinePromptBlocks(PromptBlockDefaults.outlineBlocks())
        }
    }

    // endregion

    companion object {
        fun factory(
            userPreferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NovelPromptSettingsViewModel(userPreferencesRepository) as T
                }
            }
        }
    }
}
