package org.jiangstack.mytavern.ui.interactive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.InteractiveGame
import org.jiangstack.mytavern.domain.model.WorldBook
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.InteractiveGameRepository
import org.jiangstack.mytavern.domain.repository.WorldBookRepository

class InteractiveGameEditViewModel(
    private val gameId: Long,
    private val gameRepository: InteractiveGameRepository,
    private val worldBookRepository: WorldBookRepository,
    private val characterRepository: CharacterRepository
) : ViewModel() {

    private val _game = MutableStateFlow<InteractiveGame?>(null)
    val game: StateFlow<InteractiveGame?> = _game.asStateFlow()

    val worldBooks: StateFlow<List<WorldBook>> = worldBookRepository.getAllWorldBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiCharacters: StateFlow<List<Character>> = characterRepository.getAiCharacters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadGame()
    }

    private fun loadGame() {
        viewModelScope.launch {
            _game.value = gameRepository.getGameById(gameId)
        }
    }

    fun updateGame(
        title: String,
        narratorStyle: String,
        storyBackground: String,
        storyMainPlot: String,
        windowWordCount: Int,
        playCharacterId: Long,
        worldBookId: Long?,
        characterIds: List<Long>
    ) {
        viewModelScope.launch {
            val current = _game.value ?: return@launch
            val updated = current.copy(
                title = title,
                narratorStyle = narratorStyle,
                storyBackground = storyBackground,
                storyMainPlot = storyMainPlot,
                windowWordCount = windowWordCount,
                playCharacterId = playCharacterId,
                worldBookId = worldBookId,
                characterIds = characterIds
            )
            gameRepository.updateGame(updated)
            gameRepository.setGameCharacters(gameId, characterIds)
            _game.value = updated
        }
    }

    companion object {
        fun factory(
            gameId: Long,
            gameRepository: InteractiveGameRepository,
            worldBookRepository: WorldBookRepository,
            characterRepository: CharacterRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return InteractiveGameEditViewModel(
                        gameId,
                        gameRepository,
                        worldBookRepository,
                        characterRepository
                    ) as T
                }
            }
        }
    }
}
