package org.jiangstack.mytavern.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.jiangstack.mytavern.domain.repository.LlmConfigRepository

class SettingsViewModel(
    private val llmConfigRepository: LlmConfigRepository
) : ViewModel() {
    val configs = llmConfigRepository.getAllConfigs()

    companion object {
        fun factory(repository: LlmConfigRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repository) as T
                }
            }
        }
    }
}
