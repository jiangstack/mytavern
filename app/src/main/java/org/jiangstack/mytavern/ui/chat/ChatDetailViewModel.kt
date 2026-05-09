package org.jiangstack.mytavern.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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

    private val _session = MutableStateFlow<ChatSession?>(null)
    val session: StateFlow<ChatSession?> = _session

    val aiCharacter: StateFlow<Character?> = session.flatMapLatest { session ->
        flow {
            val character = session?.aiCharacterId?.let {
                characterRepository.getCharacterById(it)
            }
            emit(character)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            _session.value = chatRepository.getSessionById(sessionId)
        }
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent

    private val _streamingReasoning = MutableStateFlow("")
    val streamingReasoning: StateFlow<String> = _streamingReasoning

    private val _thinkingEnabled = MutableStateFlow(true)
    val thinkingEnabled: StateFlow<Boolean> = _thinkingEnabled

    private var currentJob: Job? = null

    fun sendMessage(content: String) {
        if (currentJob?.isActive == true) {
            cancelCurrentRequest()
            return
        }
        currentJob = viewModelScope.launch {
            _errorMessage.value = null

            val currentSession = session.value ?: return@launch

            // 先获取当前所有消息，再加上新消息
            val currentMessages = messages.value.toMutableList()
            val userMessage = ChatMessage(
                sessionId = sessionId,
                content = content
            )
            val userMessageId = chatRepository.insertMessage(userMessage)
            currentMessages.add(userMessage.copy(id = userMessageId))

            val systemPrompt = buildSystemPrompt(currentSession)

            _isLoading.value = true
            _streamingContent.value = ""
            _streamingReasoning.value = ""

            try {
                val fullContent = StringBuilder()
                val fullReasoning = StringBuilder()
                val enableThinking = _thinkingEnabled.value
                llmService.sendChatMessageStream(
                    messages = currentMessages,
                    systemPrompt = systemPrompt,
                    thinkingEnabled = enableThinking
                ).collect { chunk ->
                    if (enableThinking && chunk.reasoningContent.isNotBlank()) {
                        fullReasoning.append(chunk.reasoningContent)
                        _streamingReasoning.value = fullReasoning.toString()
                    }
                    if (chunk.content.isNotBlank()) {
                        fullContent.append(chunk.content)
                        _streamingContent.value = fullContent.toString()
                    }
                }

                val finalContent = buildString {
                    if (enableThinking && fullReasoning.isNotEmpty()) {
                        append("<think>")
                        append(fullReasoning)
                        append("</think>")
                        appendLine()
                        appendLine()
                    }
                    append(fullContent)
                }

                val aiMessage = ChatMessage(
                    sessionId = sessionId,
                    senderId = currentSession.aiCharacterId,
                    senderName = aiCharacter.value?.name,
                    content = finalContent
                )
                chatRepository.insertMessage(aiMessage)
                _streamingContent.value = ""
                _streamingReasoning.value = ""
            } catch (e: CancellationException) {
                _streamingContent.value = ""
                _streamingReasoning.value = ""
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "请求失败"
            } finally {
                _isLoading.value = false
                currentJob = null
            }
        }
    }

    fun cancelCurrentRequest() {
        currentJob?.cancel()
        currentJob = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun toggleThinking() {
        _thinkingEnabled.value = !_thinkingEnabled.value
    }

    fun updateWorldBook(worldBookId: Long?) {
        viewModelScope.launch {
            val currentSession = _session.value ?: return@launch
            val updatedSession = currentSession.copy(worldBookId = worldBookId)
            chatRepository.updateSession(updatedSession)
            _session.value = updatedSession
        }
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
