package org.jiangstack.mytavern.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.ChatMessage
import org.jiangstack.mytavern.domain.model.ChatSession
import org.jiangstack.mytavern.domain.model.Novel
import org.jiangstack.mytavern.domain.model.NovelChapter
import org.jiangstack.mytavern.domain.repository.ChatRepository
import org.jiangstack.mytavern.domain.repository.NovelRepository
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository
import org.jiangstack.mytavern.domain.service.NovelAgentService

data class PendingWriteApproval(
    val chapterNumber: Int,
    val title: String,
    val content: String,
    val onApprove: () -> Unit,
    val onReject: () -> Unit
)

data class AgentStep(
    val type: String, // "thinking", "tool_call", "tool_result", "text"
    val content: String,
    val toolName: String? = null,
    val toolArgs: String? = null
)

class AgentChatViewModel(
    private val chatRepository: ChatRepository,
    private val novelRepository: NovelRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val novelAgentService: NovelAgentService,
    private val sessionId: Long
) : ViewModel() {

    private val _session = MutableStateFlow<ChatSession?>(null)
    val session: StateFlow<ChatSession?> = _session

    val messages: StateFlow<List<ChatMessage>> = chatRepository.getMessagesBySessionId(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _novel = MutableStateFlow<Novel?>(null)
    val novel: StateFlow<Novel?> = _novel

    private val _chapters = MutableStateFlow<List<NovelChapter>>(emptyList())
    val chapters: StateFlow<List<NovelChapter>> = _chapters

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()

    private val _streamingThinking = MutableStateFlow("")
    val streamingThinking: StateFlow<String> = _streamingThinking.asStateFlow()

    private val _agentSteps = MutableStateFlow<List<AgentStep>>(emptyList())
    val agentSteps: StateFlow<List<AgentStep>> = _agentSteps.asStateFlow()

    private val _pendingApproval = MutableStateFlow<PendingWriteApproval?>(null)
    val pendingApproval: StateFlow<PendingWriteApproval?> = _pendingApproval.asStateFlow()

    private val _currentIteration = MutableStateFlow(0)
    val currentIteration: StateFlow<Int> = _currentIteration.asStateFlow()

    private val _thinkingEnabled = MutableStateFlow(true)
    val thinkingEnabled: StateFlow<Boolean> = _thinkingEnabled.asStateFlow()

    val temperature: StateFlow<Float> = userPreferencesRepository.temperature
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val maxTokens: StateFlow<Int> = userPreferencesRepository.maxTokens
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4096)

    private var agentJob: Job? = null

    init {
        viewModelScope.launch {
            val sess = chatRepository.getSessionById(sessionId)
            _session.value = sess
            sess?.novelId?.let { novelId ->
                _novel.value = novelRepository.getNovelById(novelId)
                _chapters.value = novelRepository.getChaptersByNovelIdSync(novelId)
            }
        }
    }

    fun toggleThinkingEnabled() {
        _thinkingEnabled.value = !_thinkingEnabled.value
    }

    fun sendMessage(content: String) {
        if (agentJob?.isActive == true) {
            cancelCurrentRequest()
            return
        }

        agentJob = viewModelScope.launch {
            _errorMessage.value = null
            _streamingContent.value = ""
            _streamingThinking.value = ""
            _agentSteps.value = emptyList()
            _currentIteration.value = 0

            val currentSession = session.value ?: return@launch
            val novelId = currentSession.novelId ?: return@launch

            val userMessage = ChatMessage(
                sessionId = sessionId,
                content = content,
                messageType = "text"
            )
            val userMessageId = chatRepository.insertMessage(userMessage)
            val currentMessages = messages.value.toMutableList()
            currentMessages.add(userMessage.copy(id = userMessageId))

            val systemPrompt = currentSession.agentSystemPrompt
                ?: novel.value?.let { novel ->
                    novelAgentService.buildDefaultPrompt(novel, chapters.value)
                }
                ?: "你是一个小说创作助手。"

            _isLoading.value = true
            var hasError = false

            try {
                val steps = mutableListOf<AgentStep>()
                val fullContent = StringBuilder()
                val fullThinking = StringBuilder()
                var isThinkingPhase = false

                // 辅助函数：将累积的思考内容作为一个完整的思考块保存（仅保存到临时列表）
                suspend fun flushThinking() {
                    if (fullThinking.isNotEmpty()) {
                        steps.add(AgentStep(type = "thinking", content = fullThinking.toString()))
                        fullThinking.clear()
                        _agentSteps.value = steps.toList()
                    }
                    isThinkingPhase = false
                }

                novelAgentService.runAgentLoop(
                    novelId = novelId,
                    messages = currentMessages,
                    systemPrompt = systemPrompt,
                    thinkingEnabled = _thinkingEnabled.value,
                    temperature = temperature.value,
                    maxTokens = maxTokens.value
                ).collect { event ->
                    when (event) {
                        is NovelAgentService.AgentEvent.Thinking -> {
                            // 累积思考内容，不立即添加到 steps
                            fullThinking.append(event.content)
                            _streamingThinking.value = fullThinking.toString()
                            isThinkingPhase = true
                        }
                        is NovelAgentService.AgentEvent.TextDelta -> {
                            // 文本输出开始时，先保存之前的思考内容
                            flushThinking()
                            fullContent.append(event.content)
                            _streamingContent.value = fullContent.toString()
                        }
                        is NovelAgentService.AgentEvent.ToolCallStart -> {
                            // 工具调用开始时，先保存之前的思考内容
                            flushThinking()
                            steps.add(AgentStep(
                                type = "tool_call",
                                content = "调用工具: ${event.toolName}",
                                toolName = event.toolName,
                                toolArgs = event.args
                            ))
                            _agentSteps.value = steps.toList()
                        }
                        is NovelAgentService.AgentEvent.ToolResult -> {
                            steps.add(AgentStep(
                                type = "tool_result",
                                content = event.result,
                                toolName = event.toolName
                            ))
                            _agentSteps.value = steps.toList()
                        }
                        is NovelAgentService.AgentEvent.WriteApprovalNeeded -> {
                            _pendingApproval.value = PendingWriteApproval(
                                chapterNumber = event.chapterNumber,
                                title = event.title,
                                content = event.content,
                                onApprove = {
                                    event.deferred.complete(true)
                                    _pendingApproval.value = null
                                },
                                onReject = {
                                    event.deferred.complete(false)
                                    _pendingApproval.value = null
                                }
                            )
                        }
                        is NovelAgentService.AgentEvent.FinalResponse -> {
                            // 最终响应前，先保存思考内容
                            flushThinking()

                            // 一次性保存所有步骤到数据库（避免运行时重复显示）
                            for (step in steps) {
                                chatRepository.insertMessage(ChatMessage(
                                    sessionId = sessionId,
                                    content = when (step.type) {
                                        "tool_call" -> "调用工具: ${step.toolName}\n参数: ${step.toolArgs}"
                                        else -> step.content
                                    },
                                    role = "assistant",
                                    messageType = step.type
                                ))
                            }

                            if (event.content.isNotBlank()) {
                                val aiMessage = ChatMessage(
                                    sessionId = sessionId,
                                    content = event.content,
                                    role = "assistant",
                                    messageType = "text"
                                )
                                chatRepository.insertMessage(aiMessage)
                            }
                            _streamingContent.value = ""
                            _streamingThinking.value = ""
                        }
                        is NovelAgentService.AgentEvent.Error -> {
                            flushThinking()
                            hasError = true
                            _errorMessage.value = event.message
                        }
                    }
                }
            } catch (e: CancellationException) {
                // 用户取消
            } catch (e: Exception) {
                hasError = true
                _errorMessage.value = e.message ?: "智能体运行失败"
            } finally {
                _isLoading.value = false
                if (!hasError) {
                    _streamingContent.value = ""
                    _streamingThinking.value = ""
                }
            }
        }
    }

    fun cancelCurrentRequest() {
        agentJob?.cancel()
        agentJob = null
        _isLoading.value = false
        _pendingApproval.value?.onReject()
        _pendingApproval.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearMessages() {
        viewModelScope.launch {
            chatRepository.deleteMessagesBySessionId(sessionId)
            _agentSteps.value = emptyList()
            _streamingContent.value = ""
            _streamingThinking.value = ""
        }
    }

    fun updateBackgroundUri(backgroundUri: String?) {
        viewModelScope.launch {
            val currentSession = _session.value ?: return@launch
            val updatedSession = currentSession.copy(backgroundUri = backgroundUri)
            chatRepository.updateSession(updatedSession)
            _session.value = updatedSession
        }
    }

    companion object {
        fun factory(
            chatRepository: ChatRepository,
            novelRepository: NovelRepository,
            userPreferencesRepository: UserPreferencesRepository,
            novelAgentService: NovelAgentService,
            sessionId: Long
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AgentChatViewModel(
                        chatRepository,
                        novelRepository,
                        userPreferencesRepository,
                        novelAgentService,
                        sessionId
                    ) as T
                }
            }
        }
    }
}
