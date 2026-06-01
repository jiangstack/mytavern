package org.jiangstack.mytavern.ui.novel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.Novel
import org.jiangstack.mytavern.domain.model.WorldBook
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.NovelRepository
import org.jiangstack.mytavern.domain.repository.WorldBookRepository

class NovelListViewModel(
    private val novelRepository: NovelRepository,
    private val worldBookRepository: WorldBookRepository,
    private val characterRepository: CharacterRepository
) : ViewModel() {

    val novels: StateFlow<List<Novel>> = novelRepository.getAllNovels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val worldBooks: StateFlow<List<WorldBook>> = worldBookRepository.getAllWorldBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiCharacters: StateFlow<List<Character>> = characterRepository.getAiCharacters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createNovel(title: String, description: String, worldBookId: Long?, characterIds: List<Long>) {
        viewModelScope.launch {
            val novel = Novel(
                title = title,
                description = description,
                worldBookId = worldBookId,
                characterIds = characterIds
            )
            novelRepository.insertNovel(novel)
        }
    }

    fun deleteNovel(novel: Novel) {
        viewModelScope.launch {
            novelRepository.deleteChaptersByNovelId(novel.id)
            novelRepository.deleteNovel(novel)
        }
    }

    companion object {
        fun factory(
            novelRepository: NovelRepository,
            worldBookRepository: WorldBookRepository,
            characterRepository: CharacterRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NovelListViewModel(
                        novelRepository,
                        worldBookRepository,
                        characterRepository
                    ) as T
                }
            }
        }
    }
}
