package org.jiangstack.mytavern.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.ChatMessage
import org.jiangstack.mytavern.domain.model.ChatSession
import org.jiangstack.mytavern.domain.model.WorldBook
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.ChatRepository
import org.jiangstack.mytavern.domain.repository.WorldBookRepository
import org.jiangstack.mytavern.domain.service.LlmService

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatDetailViewModel(
    private val chatRepository: ChatRepository,
    private val characterRepository: CharacterRepository,
    private val worldBookRepository: WorldBookRepository,
    private val llmService: LlmService,
    private val sessionId: Long
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = chatRepository.getMessagesBySessionId(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val session: StateFlow<ChatSession?> = kotlinx.coroutines.flow.flow {
        emit(chatRepository.getSessionById(sessionId))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val aiCharacter: StateFlow<Character?> = session.flatMapLatest { session ->
        flow {
            val character = session?.aiCharacterId?.let {
                characterRepository.getCharacterById(it)
            }
            emit(character)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun sendMessage(content: String) {
        viewModelScope.launch {
            _errorMessage.value = null

            val currentSession = session.value ?: return@launch

            val userMessage = ChatMessage(
                sessionId = sessionId,
                content = content
            )
            chatRepository.insertMessage(userMessage)

            val systemPrompt = buildSystemPrompt(currentSession)

            _isLoading.value = true
            val result = llmService.sendChatMessage(
                messages = messages.value,
                systemPrompt = systemPrompt
            )
            _isLoading.value = false

            result.onSuccess { reply ->
                val aiMessage = ChatMessage(
                    sessionId = sessionId,
                    senderId = currentSession.aiCharacterId,
                    senderName = aiCharacter.value?.name,
                    content = reply
                )
                chatRepository.insertMessage(aiMessage)
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "请求失败"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private suspend fun buildSystemPrompt(session: ChatSession): String {
        val character = session.aiCharacterId?.let {
            characterRepository.getCharacterById(it)
        } ?: return ""

        val worldBook = session.worldBookId?.let {
            worldBookRepository.getWorldBookById(it)
        }

        return buildString {
            appendLine("你是 ${character.name}。")
            appendLine("角色描述：${character.description}")
            if (worldBook != null) {
                appendLine()
                appendLine("【世界书：${worldBook.name}】")
                appendLine(worldBook.description)
                worldBook.rules.forEach { rule ->
                    appendLine("- ${rule.name}: ${rule.description}")
                }
            }
        }
    }

    companion object {
        fun factory(
            chatRepository: ChatRepository,
            characterRepository: CharacterRepository,
            worldBookRepository: WorldBookRepository,
            llmService: LlmService,
            sessionId: Long
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChatDetailViewModel(
                        chatRepository,
                        characterRepository,
                        worldBookRepository,
                        llmService,
                        sessionId
                    ) as T
                }
            }
        }
    }
}
