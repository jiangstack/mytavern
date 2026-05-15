package org.jiangstack.mytavern.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.ChatMessage
import org.jiangstack.mytavern.domain.model.ChatSession
import org.jiangstack.mytavern.domain.model.SessionType
import org.jiangstack.mytavern.domain.model.WorldBook
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.ChatRepository
import org.jiangstack.mytavern.domain.repository.SessionCharacterRepository
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository
import org.jiangstack.mytavern.domain.repository.WorldBookRepository
import org.jiangstack.mytavern.domain.service.LlmService

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatDetailViewModel(
    private val chatRepository: ChatRepository,
    private val characterRepository: CharacterRepository,
    private val worldBookRepository: WorldBookRepository,
    private val sessionCharacterRepository: SessionCharacterRepository,
    private val llmService: LlmService,
    private val userPreferencesRepository: UserPreferencesRepository,
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

    val groupCharacters: StateFlow<List<Character>> = _session.flatMapLatest { session ->
        if (session?.type == SessionType.GROUP) {
            sessionCharacterRepository.getCharactersBySessionId(sessionId)
                .map { ids ->
                    ids.mapNotNull { characterRepository.getCharacterById(it) }
                }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyCount: StateFlow<Int> = userPreferencesRepository.chatHistoryCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 12)

    val temperature: StateFlow<Float> = userPreferencesRepository.temperature
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

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

    private val _thinkingEnabled = MutableStateFlow(false)
    val thinkingEnabled: StateFlow<Boolean> = _thinkingEnabled

    data class StreamingState(
        val content: String = "",
        val reasoning: String = ""
    )

    private val _streamingStates = MutableStateFlow<Map<Long, StreamingState>>(emptyMap())
    val streamingStates: StateFlow<Map<Long, StreamingState>> = _streamingStates

    private val _activeGeneratingIds = MutableStateFlow<Set<Long>>(emptySet())
    val activeGeneratingIds: StateFlow<Set<Long>> = _activeGeneratingIds

    private var singleChatJob: Job? = null
    private val groupChatJobs = mutableMapOf<Long, Job>()

    fun sendMessage(content: String) {
        if (singleChatJob?.isActive == true || groupChatJobs.any { it.value.isActive }) {
            cancelCurrentRequests()
            return
        }

        singleChatJob = viewModelScope.launch {
            _errorMessage.value = null

            val currentSession = session.value ?: return@launch
            val chars = groupCharacters.value

            val userMessage = ChatMessage(
                sessionId = sessionId,
                content = content
            )
            val userMessageId = chatRepository.insertMessage(userMessage)
            val currentMessages = messages.value.toMutableList()
            currentMessages.add(userMessage.copy(id = userMessageId))

            if (currentSession.type == SessionType.SINGLE) {
                sendSingleMessage(currentSession, currentMessages)
            } else {
                val mentioned = parseMentions(content, chars)
                val targetChars = when {
                    mentioned.isNotEmpty() -> mentioned
                    else -> {
                        val lastAiMessage = messages.value.findLast { it.senderId != null }
                        val lastChar = lastAiMessage?.senderId?.let { id ->
                            chars.find { it.id == id }
                        }
                        listOfNotNull(lastChar ?: chars.firstOrNull())
                    }
                }

                targetChars.forEach { character ->
                    val job = viewModelScope.launch {
                        sendGroupMessageForCharacter(character, chars, currentMessages)
                    }
                    groupChatJobs[character.id] = job
                }
            }
        }
    }

    private suspend fun sendSingleMessage(session: ChatSession, currentMessages: List<ChatMessage>) {
        val systemPrompt = buildSystemPrompt(session)

        _isLoading.value = true
        _streamingContent.value = ""
        _streamingReasoning.value = ""

        try {
            val fullContent = StringBuilder()
            val fullReasoning = StringBuilder()
            val enableThinking = _thinkingEnabled.value
            val messagesToSend = currentMessages.takeLast(historyCount.value)
            llmService.sendChatMessageStream(
                messages = messagesToSend,
                systemPrompt = systemPrompt,
                thinkingEnabled = enableThinking,
                temperature = temperature.value
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
                senderId = session.aiCharacterId,
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
            singleChatJob = null
        }
    }

    private suspend fun sendGroupMessageForCharacter(
        targetCharacter: Character,
        allCharacters: List<Character>,
        currentMessages: List<ChatMessage>
    ) {
        _activeGeneratingIds.value += targetCharacter.id
        _streamingStates.value += (targetCharacter.id to StreamingState())

        val systemPrompt = buildGroupSystemPrompt(targetCharacter, allCharacters)

        val instructionMessage = ChatMessage(
            sessionId = sessionId,
            senderId = null,
            content = "请输出${targetCharacter.name}的回复"
        )
        val historyMessages = currentMessages.takeLast(historyCount.value)
        val allMessages = historyMessages + instructionMessage

        try {
            val fullContent = StringBuilder()
            val fullReasoning = StringBuilder()
            val enableThinking = _thinkingEnabled.value

            llmService.sendChatMessageStream(
                messages = allMessages,
                systemPrompt = systemPrompt,
                thinkingEnabled = enableThinking,
                isGroupChat = true,
                temperature = temperature.value
            ).collect { chunk ->
                if (enableThinking && chunk.reasoningContent.isNotBlank()) {
                    fullReasoning.append(chunk.reasoningContent)
                }
                if (chunk.content.isNotBlank()) {
                    fullContent.append(chunk.content)
                }
                _streamingStates.value = _streamingStates.value + (targetCharacter.id to StreamingState(
                    content = fullContent.toString(),
                    reasoning = fullReasoning.toString()
                ))
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
                senderId = targetCharacter.id,
                senderName = targetCharacter.name,
                content = finalContent
            )
            chatRepository.insertMessage(aiMessage)
        } catch (_: CancellationException) {
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "请求失败"
        } finally {
            _activeGeneratingIds.value -= targetCharacter.id
            _streamingStates.value = _streamingStates.value - targetCharacter.id
            groupChatJobs.remove(targetCharacter.id)
        }
    }

    fun cancelCurrentRequests() {
        singleChatJob?.cancel()
        singleChatJob = null
        groupChatJobs.values.forEach { it.cancel() }
        groupChatJobs.clear()
        _activeGeneratingIds.value = emptySet()
        _streamingStates.value = emptyMap()
        _isLoading.value = false
        _streamingContent.value = ""
        _streamingReasoning.value = ""
    }

    fun triggerCharacterReply(characterId: Long) {
        if (groupChatJobs.any { it.value.isActive }) return

        viewModelScope.launch {
            val chars = groupCharacters.value
            val targetChar = chars.find { it.id == characterId } ?: return@launch
            val currentMessages = messages.value
            sendGroupMessageForCharacter(targetChar, chars, currentMessages)
        }
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

    fun regenerateMessage(message: ChatMessage) {
        if (message.senderId == null) return
        singleChatJob = viewModelScope.launch {
            chatRepository.deleteMessagesAfter(message.sessionId, message.timestamp)
            val currentSession = session.value ?: return@launch
            val currentMessages = chatRepository.getMessagesBySessionId(sessionId).stateIn(viewModelScope).value
            val messagesToSend = currentMessages.takeLast(historyCount.value)

            if (currentSession.type == SessionType.SINGLE) {
                val systemPrompt = buildSystemPrompt(currentSession)
                _isLoading.value = true
                _streamingContent.value = ""
                _streamingReasoning.value = ""

                try {
                    val fullContent = StringBuilder()
                    val fullReasoning = StringBuilder()
                    val enableThinking = _thinkingEnabled.value
                    llmService.sendChatMessageStream(
                        messages = messagesToSend,
                        systemPrompt = systemPrompt,
                        thinkingEnabled = enableThinking,
                        temperature = temperature.value
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
                    singleChatJob = null
                }
            } else {
                val chars = groupCharacters.value
                val targetChar = chars.find { it.id == message.senderId } ?: return@launch
                sendGroupMessageForCharacter(targetChar, chars, currentMessages)
            }
        }
    }

    fun editMessage(message: ChatMessage, newContent: String) {
        if (message.senderId != null) return
        viewModelScope.launch {
            chatRepository.updateMessage(message.copy(content = newContent))
            chatRepository.deleteMessagesAfter(message.sessionId, message.timestamp)
            sendMessage(newContent)
        }
    }

    fun deleteMessage(message: ChatMessage) {
        viewModelScope.launch {
            chatRepository.deleteMessage(message)
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

    private suspend fun buildGroupSystemPrompt(
        targetCharacter: Character,
        allCharacters: List<Character>
    ): String {
        val worldBook = session.value?.worldBookId?.let {
            worldBookRepository.getWorldBookById(it)
        }

        return buildString {
            appendLine("你是 ${targetCharacter.name}。")
            appendLine("角色描述：${targetCharacter.description}")
            appendLine()
            appendLine("当前群聊中的其他成员：")
            allCharacters
                .filter { it.id != targetCharacter.id }
                .forEach { appendLine("- ${it.name}：${it.description}") }
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

    private fun parseMentions(content: String, characters: List<Character>): List<Character> {
        val mentionPattern = Regex("@([^\\s@]+)")
        val mentionNames = mentionPattern.findAll(content).map { it.groupValues[1] }.toList()

        val matched = characters.filter { char ->
            mentionNames.any { mention ->
                char.name.contains(mention) || mention.contains(char.name)
            }
        }

        return matched.ifEmpty {
            characters.filter { char -> content.contains(char.name) }
        }
    }

    companion object {
        fun factory(
            chatRepository: ChatRepository,
            characterRepository: CharacterRepository,
            worldBookRepository: WorldBookRepository,
            sessionCharacterRepository: SessionCharacterRepository,
            llmService: LlmService,
            userPreferencesRepository: UserPreferencesRepository,
            sessionId: Long
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChatDetailViewModel(
                        chatRepository,
                        characterRepository,
                        worldBookRepository,
                        sessionCharacterRepository,
                        llmService,
                        userPreferencesRepository,
                        sessionId
                    ) as T
                }
            }
        }
    }
}
