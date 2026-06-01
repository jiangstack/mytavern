package org.jiangstack.mytavern.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.ChatMessage
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository
import org.jiangstack.mytavern.domain.service.LlmService

class CharacterListViewModel(
    private val characterRepository: CharacterRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val llmService: LlmService
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    // AI 生成状态
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _aiGeneratedName = MutableStateFlow("")
    val aiGeneratedName: StateFlow<String> = _aiGeneratedName.asStateFlow()

    private val _aiGeneratedDescription = MutableStateFlow("")
    val aiGeneratedDescription: StateFlow<String> = _aiGeneratedDescription.asStateFlow()

    private val _generateError = MutableStateFlow<String?>(null)
    val generateError: StateFlow<String?> = _generateError.asStateFlow()

    val characters: StateFlow<List<Character>> = characterRepository.getAllCharacters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultUserCharacterId = userPreferencesRepository.defaultUserCharacterId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    fun generateCharacter(prompt: String) {
        if (_isGenerating.value || prompt.isBlank()) return

        viewModelScope.launch {
            _isGenerating.value = true
            _generateError.value = null
            _aiGeneratedName.value = ""
            _aiGeneratedDescription.value = ""

            try {
                val systemPrompt = buildString {
                    appendLine("你是一位角色设计助手。请根据用户的一句话描述，生成一个角色。")
                    appendLine()
                    appendLine("要求：")
                    appendLine("1. 角色名称：简洁有力，2-4个字")
                    appendLine("2. 角色描述：100-200字，包含性格特点、背景故事、说话风格等")
                    appendLine()
                    appendLine("请以 JSON 格式输出：")
                    appendLine("{\"name\": \"角色名\", \"description\": \"角色描述\"}")
                    appendLine()
                    appendLine("只输出 JSON，不要有其他内容。")
                }

                val userMessage = ChatMessage(
                    sessionId = 0,
                    content = prompt,
                    role = "user"
                )

                val temperature = userPreferencesRepository.temperature.first()
                val maxTokens = userPreferencesRepository.maxTokens.first()

                val result = llmService.sendChatMessage(
                    messages = listOf(userMessage),
                    systemPrompt = systemPrompt,
                    temperature = temperature,
                    maxTokens = maxTokens,
                    thinkingEnabled = false
                )

                result.fold(
                    onSuccess = { response ->
                        try {
                            // 提取 JSON 内容（可能被 markdown 代码块包裹）
                            val jsonStr = extractJson(response)
                            val parsed = json.decodeFromString<CharacterGenerated>(jsonStr)
                            _aiGeneratedName.value = parsed.name
                            _aiGeneratedDescription.value = parsed.description
                        } catch (e: Exception) {
                            _generateError.value = "解析生成结果失败"
                        }
                    },
                    onFailure = { error ->
                        _generateError.value = error.message ?: "生成失败"
                    }
                )
            } catch (e: Exception) {
                _generateError.value = e.message ?: "生成失败"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun extractJson(text: String): String {
        // 尝试提取 markdown 代码块中的 JSON
        val codeBlockRegex = "```(?:json)?\\s*\\n?(.*?)\\n?```".toRegex(RegexOption.DOT_MATCHES_ALL)
        val match = codeBlockRegex.find(text)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        // 如果没有代码块，尝试直接找 JSON
        val jsonRegex = "\\{[^{}]*\"name\"[^{}]*\"description\"[^{}]*\\}".toRegex(RegexOption.DOT_MATCHES_ALL)
        val jsonMatch = jsonRegex.find(text)
        if (jsonMatch != null) {
            return jsonMatch.value
        }
        return text.trim()
    }

    fun clearGenerateError() {
        _generateError.value = null
    }

    fun resetGeneratedFields() {
        _aiGeneratedName.value = ""
        _aiGeneratedDescription.value = ""
    }

    @kotlinx.serialization.Serializable
    private data class CharacterGenerated(
        val name: String,
        val description: String
    )

    companion object {
        fun factory(
            characterRepository: CharacterRepository,
            userPreferencesRepository: UserPreferencesRepository,
            llmService: LlmService
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CharacterListViewModel(characterRepository, userPreferencesRepository, llmService) as T
                }
            }
        }
    }
}
