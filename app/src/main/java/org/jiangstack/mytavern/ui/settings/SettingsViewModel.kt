package org.jiangstack.mytavern.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.data.repository.BackupRepository
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.CharacterType
import org.jiangstack.mytavern.domain.model.ImageApiConfig
import org.jiangstack.mytavern.domain.model.LlmConfig
import org.jiangstack.mytavern.domain.model.QuickReply
import org.jiangstack.mytavern.domain.model.ThemeMode
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.ImageApiConfigRepository
import org.jiangstack.mytavern.domain.repository.LlmConfigRepository
import org.jiangstack.mytavern.domain.repository.QuickReplyRepository
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository
sealed class BackupState {
    data object Idle : BackupState()
    data object Exporting : BackupState()
    data object Importing : BackupState()
    data class Success(val message: String) : BackupState()
    data class Error(val message: String) : BackupState()
}
class SettingsViewModel(
    private val llmConfigRepository: LlmConfigRepository,
    private val imageApiConfigRepository: ImageApiConfigRepository,
    private val characterRepository: CharacterRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val quickReplyRepository: QuickReplyRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    val configs: StateFlow<List<LlmConfig>> = llmConfigRepository.getAllConfigs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val defaultLlmConfigId: StateFlow<Long?> = userPreferencesRepository.defaultLlmConfigId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val imageApiConfigs: StateFlow<List<ImageApiConfig>> = imageApiConfigRepository.getAllConfigs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultImageApiConfigId: StateFlow<Long?> = userPreferencesRepository.defaultImageApiConfigId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val themeMode: StateFlow<ThemeMode> = userPreferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val defaultUserCharacter = combine(
        userPreferencesRepository.defaultUserCharacterId,
        characterRepository.getUserCharacters()
    ) { defaultId, userChars ->
        userChars.find { it.id == defaultId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userCharacters = characterRepository.getUserCharacters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatHistoryCount: StateFlow<Int> = userPreferencesRepository.chatHistoryCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 12)

    val temperature: StateFlow<Float> = userPreferencesRepository.temperature
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val maxTokens: StateFlow<Int> = userPreferencesRepository.maxTokens
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4096)

    val quickReplies: StateFlow<List<QuickReply>> = quickReplyRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    val dialogueHighlightEnabled: StateFlow<Boolean> = userPreferencesRepository.dialogueHighlightEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dialogueHighlightColor: StateFlow<Long> = userPreferencesRepository.dialogueHighlightColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF4FC3F7L)

    val interactiveMaxIterations: StateFlow<Int> = userPreferencesRepository.interactiveMaxIterations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    fun setDefaultUserCharacter(id: Long?) {
        viewModelScope.launch {
            userPreferencesRepository.setDefaultUserCharacterId(id)
        }
    }

    fun saveConfig(config: LlmConfig) {
        viewModelScope.launch {
            if (config.id == 0L) {
                llmConfigRepository.insertConfig(config)
            } else {
                llmConfigRepository.updateConfig(config)
            }
        }
    }

    fun deleteConfig(config: LlmConfig) {
        viewModelScope.launch {
            llmConfigRepository.deleteConfig(config)
        }
    }

    fun copyConfig(config: LlmConfig, suffix: String) {
        viewModelScope.launch {
            llmConfigRepository.insertConfig(
                config.copy(id = 0, name = "${config.name}$suffix")
            )
        }
    }

    fun setDefaultLlmConfig(id: Long?) {
        viewModelScope.launch {
            userPreferencesRepository.setDefaultLlmConfigId(id)
        }
    }

    fun saveImageApiConfig(config: ImageApiConfig) {
        viewModelScope.launch {
            if (config.id == 0L) {
                imageApiConfigRepository.insertConfig(config)
            } else {
                imageApiConfigRepository.updateConfig(config)
            }
        }
    }

    fun deleteImageApiConfig(config: ImageApiConfig) {
        viewModelScope.launch {
            imageApiConfigRepository.deleteConfig(config)
        }
    }

    fun copyImageApiConfig(config: ImageApiConfig, suffix: String) {
        viewModelScope.launch {
            imageApiConfigRepository.insertConfig(
                config.copy(id = 0, name = "${config.name}$suffix")
            )
        }
    }

    fun setDefaultImageApiConfig(id: Long?) {
        viewModelScope.launch {
            userPreferencesRepository.setDefaultImageApiConfigId(id)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    fun setChatHistoryCount(count: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setChatHistoryCount(count.coerceIn(1, 50))
        }
    }

    fun setTemperature(temp: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setTemperature(temp.coerceIn(0.0f, 2.0f))
        }
    }

    fun setMaxTokens(value: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setMaxTokens(value.coerceIn(256, 32768))
        }
    }

    fun setDialogueHighlightEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setDialogueHighlightEnabled(enabled) }
    }

    fun setDialogueHighlightColor(color: Long) {
        viewModelScope.launch { userPreferencesRepository.setDialogueHighlightColor(color) }
    }

    fun setInteractiveMaxIterations(count: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setInteractiveMaxIterations(count.coerceIn(1, 10))
        }
    }

    fun saveQuickReply(quickReply: QuickReply) {
        viewModelScope.launch {
            if (quickReply.id == 0L) {
                quickReplyRepository.insert(quickReply)
            } else {
                quickReplyRepository.update(quickReply)
            }
        }
    }

    fun deleteQuickReply(quickReply: QuickReply) {
        viewModelScope.launch {
            quickReplyRepository.delete(quickReply)
        }
    }

    fun exportData(outputUri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.Exporting
            val result = backupRepository.exportToZip(outputUri)
            _backupState.value = result.fold(
                onSuccess = { BackupState.Success(it) },
                onFailure = { BackupState.Error(it.message ?: "导出失败") }
            )
        }
    }

    fun importData(zipUri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.Importing
            val result = backupRepository.importFromZip(zipUri)
            _backupState.value = result.fold(
                onSuccess = { BackupState.Success(it) },
                onFailure = { BackupState.Error(it.message ?: "导入失败") }
            )
        }
    }

    fun resetBackupState() {
        _backupState.value = BackupState.Idle
    }

    companion object {
        fun factory(
            llmConfigRepository: LlmConfigRepository,
            imageApiConfigRepository: ImageApiConfigRepository,
            characterRepository: CharacterRepository,
            userPreferencesRepository: UserPreferencesRepository,
            quickReplyRepository: QuickReplyRepository,
            backupRepository: BackupRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        llmConfigRepository,
                        imageApiConfigRepository,
                        characterRepository,
                        userPreferencesRepository,
                        quickReplyRepository,
                        backupRepository
                    ) as T
                }
            }
        }
    }
}
