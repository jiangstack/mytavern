package org.jiangstack.mytavern.ui.interactive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.InteractiveGame
import org.jiangstack.mytavern.domain.model.InteractiveGameState
import org.jiangstack.mytavern.domain.model.InteractiveMessage
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.InteractiveGameRepository
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository
import org.jiangstack.mytavern.domain.service.InteractiveStoryService

class InteractiveGamePlayViewModel(
    private val gameId: Long,
    private val gameRepository: InteractiveGameRepository,
    private val characterRepository: CharacterRepository,
    private val storyService: InteractiveStoryService,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _game = MutableStateFlow<InteractiveGame?>(null)
    val game: StateFlow<InteractiveGame?> = _game.asStateFlow()

    private val _characters = MutableStateFlow<List<Character>>(emptyList())
    val characters: StateFlow<List<Character>> = _characters.asStateFlow()

    private val _messages = MutableStateFlow<List<InteractiveMessage>>(emptyList())
    val messages: StateFlow<List<InteractiveMessage>> = _messages.asStateFlow()

    private val _gameState = MutableStateFlow<InteractiveGameState?>(null)
    val gameState: StateFlow<InteractiveGameState?> = _gameState.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _currentStoryText = MutableStateFlow("")
    val currentStoryText: StateFlow<String> = _currentStoryText.asStateFlow()

    private val _actionOptions = MutableStateFlow<List<String>>(emptyList())
    val actionOptions: StateFlow<List<String>> = _actionOptions.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val storyWordCount: StateFlow<Int> = combine(_messages, _currentStoryText) { messages, current ->
        messages.filter { it.role != "user" }.sumOf { it.content.length } + current.length
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val contextBoundaryIndex: StateFlow<Int> = combine(_messages, _game) { messages, game ->
        val window = game?.windowWordCount ?: 3000
        val displayed = messages.filter { it.role != "user" }
        val total = displayed.sumOf { it.content.length }
        if (total <= window || displayed.isEmpty()) -1
        else {
            var count = 0
            var idx = displayed.lastIndex
            while (idx >= 0) {
                if (count + displayed[idx].content.length > window && idx < displayed.lastIndex) break
                count += displayed[idx].content.length
                idx--
            }
            idx + 1
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

    val dialogueHighlightEnabled: StateFlow<Boolean> = userPreferencesRepository.dialogueHighlightEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dialogueHighlightColor: StateFlow<Long> = userPreferencesRepository.dialogueHighlightColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF4FC3F7L)

    private var streamingJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val loadedGame = gameRepository.getGameById(gameId)
            _game.value = loadedGame
            loadedGame?.let {
                _characters.value = characterRepository.getCharactersByIds(
                    (it.characterIds + it.playCharacterId).distinct()
                )
            }
        }
        viewModelScope.launch {
            gameRepository.getMessagesByGameId(gameId).collect {
                _messages.value = it
            }
        }
        viewModelScope.launch {
            gameRepository.getGameStateByGameId(gameId).collect {
                _gameState.value = it
            }
        }
    }

    fun startNewTurn(userAction: String) {
        executeStoryTurn(userAction, saveUserMessage = true)
    }

    fun continueStory() {
        executeStoryTurn("继续", saveUserMessage = false)
    }

    private fun executeStoryTurn(userAction: String, saveUserMessage: Boolean) {
        if (_isStreaming.value) return
        if (userAction.isBlank()) return

        val currentGame = _game.value ?: return

        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            _isStreaming.value = true
            _currentStoryText.value = ""
            _actionOptions.value = emptyList()
            _error.value = null

            if (saveUserMessage) {
                val userMessage = InteractiveMessage(
                    gameId = gameId,
                    role = "user",
                    content = userAction
                )
                gameRepository.insertMessage(userMessage)
            }

            val storyBuilder = StringBuilder()
            try {
                storyService.runStoryTurn(
                    game = currentGame,
                    messages = _messages.value,
                    gameState = _gameState.value,
                    userAction = userAction,
                    freeMode = !saveUserMessage
                ).collect { event ->
                    when (event) {
                        is InteractiveStoryService.StoryEvent.TextDelta -> {
                            storyBuilder.append(event.content)
                            _currentStoryText.value = storyBuilder.toString()
                        }
                        is InteractiveStoryService.StoryEvent.ToolResult -> {
                            val state = gameRepository.getGameStateByGameIdSync(gameId)
                            _gameState.value = state
                        }
                        is InteractiveStoryService.StoryEvent.ActionOptions -> {
                            _actionOptions.value = event.options
                        }
                        is InteractiveStoryService.StoryEvent.Error -> {
                            _error.value = event.message
                        }
                        is InteractiveStoryService.StoryEvent.FinalResponse -> {
                            if (storyBuilder.isNotEmpty()) {
                                val narratorMessage = InteractiveMessage(
                                    gameId = gameId,
                                    role = "narrator",
                                    content = storyBuilder.toString(),
                                    actionOptions = _actionOptions.value
                                )
                                gameRepository.insertMessage(narratorMessage)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "未知错误"
            } finally {
                if (_error.value != null && storyBuilder.isNotEmpty()) {
                    val narratorMessage = InteractiveMessage(
                        gameId = gameId,
                        role = "narrator",
                        content = storyBuilder.toString(),
                        actionOptions = _actionOptions.value
                    )
                    gameRepository.insertMessage(narratorMessage)
                }
                _isStreaming.value = false
                _currentStoryText.value = ""
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun updateGameState(environment: String, characterStatus: String, characterItems: String) {
        viewModelScope.launch {
            val state = InteractiveGameState(
                gameId = gameId,
                environment = environment,
                characterStatus = characterStatus,
                characterItems = characterItems
            )
            gameRepository.insertOrUpdateGameState(state)
            _gameState.value = state
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        _isStreaming.value = false
    }

    fun clearStory() {
        viewModelScope.launch {
            streamingJob?.cancelAndJoin()
            _isStreaming.value = false
            _currentStoryText.value = ""
            _actionOptions.value = emptyList()
            _error.value = null
            gameRepository.deleteMessagesByGameId(gameId)
        }
    }

    suspend fun getCharacterById(id: Long): Character? {
        return characterRepository.getCharacterById(id)
    }

    companion object {
        fun factory(
            gameId: Long,
            gameRepository: InteractiveGameRepository,
            characterRepository: CharacterRepository,
            storyService: InteractiveStoryService,
            userPreferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return InteractiveGamePlayViewModel(
                        gameId,
                        gameRepository,
                        characterRepository,
                        storyService,
                        userPreferencesRepository
                    ) as T
                }
            }
        }
    }
}
