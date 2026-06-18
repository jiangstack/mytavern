package org.jiangstack.mytavern.ui.interactive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.InteractiveGame
import org.jiangstack.mytavern.domain.model.WorldBook
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.InteractiveGameRepository
import org.jiangstack.mytavern.domain.repository.WorldBookRepository

class InteractiveGameListViewModel(
    private val gameRepository: InteractiveGameRepository,
    private val worldBookRepository: WorldBookRepository,
    private val characterRepository: CharacterRepository
) : ViewModel() {

    val games: StateFlow<List<InteractiveGame>> = gameRepository.getAllGames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val worldBooks: StateFlow<List<WorldBook>> = worldBookRepository.getAllWorldBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiCharacters: StateFlow<List<Character>> = characterRepository.getAiCharacters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createGame(
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
            val game = InteractiveGame(
                title = title,
                narratorStyle = narratorStyle,
                storyBackground = storyBackground,
                storyMainPlot = storyMainPlot,
                windowWordCount = windowWordCount,
                playCharacterId = playCharacterId,
                worldBookId = worldBookId,
                characterIds = characterIds
            )
            gameRepository.insertGame(game)
        }
    }

    fun deleteGame(game: InteractiveGame) {
        viewModelScope.launch {
            gameRepository.deleteGame(game)
        }
    }

    companion object {
        fun factory(
            gameRepository: InteractiveGameRepository,
            worldBookRepository: WorldBookRepository,
            characterRepository: CharacterRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return InteractiveGameListViewModel(
                        gameRepository,
                        worldBookRepository,
                        characterRepository
                    ) as T
                }
            }
        }
    }
}
