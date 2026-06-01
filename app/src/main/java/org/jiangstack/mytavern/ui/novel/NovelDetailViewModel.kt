package org.jiangstack.mytavern.ui.novel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.Novel
import org.jiangstack.mytavern.domain.model.NovelChapter
import org.jiangstack.mytavern.domain.model.WorldBook
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.NovelRepository
import org.jiangstack.mytavern.domain.repository.WorldBookRepository

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NovelDetailViewModel(
    private val novelRepository: NovelRepository,
    private val worldBookRepository: WorldBookRepository,
    private val characterRepository: CharacterRepository,
    private val novelId: Long
) : ViewModel() {

    private val _novel = MutableStateFlow<Novel?>(null)
    val novel: StateFlow<Novel?> = _novel

    val chapters: StateFlow<List<NovelChapter>> = _novel.flatMapLatest { n ->
        if (n != null) novelRepository.getChaptersByNovelId(novelId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val worldBook: StateFlow<WorldBook?> = _novel.flatMapLatest { n ->
        flow {
            emit(n?.worldBookId?.let { worldBookRepository.getWorldBookById(it) })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val characters: StateFlow<List<Character>> = _novel.flatMapLatest { n ->
        flow {
            val chars = n?.characterIds?.mapNotNull { characterRepository.getCharacterById(it) } ?: emptyList()
            emit(chars)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWorldBooks: StateFlow<List<WorldBook>> = worldBookRepository.getAllWorldBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAiCharacters: StateFlow<List<Character>> = characterRepository.getAiCharacters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _novel.value = novelRepository.getNovelById(novelId)
        }
    }

    fun updateNovel(title: String, description: String, worldBookId: Long?, characterIds: List<Long>) {
        viewModelScope.launch {
            val current = _novel.value ?: return@launch
            val updated = current.copy(
                title = title,
                description = description,
                worldBookId = worldBookId,
                characterIds = characterIds,
                updatedAt = System.currentTimeMillis()
            )
            novelRepository.updateNovel(updated)
            novelRepository.setNovelCharacters(novelId, characterIds)
            _novel.value = novelRepository.getNovelById(novelId)
        }
    }

    fun addChapter(title: String, outline: String) {
        viewModelScope.launch {
            val currentChapters = chapters.value
            val nextNumber = (currentChapters.maxOfOrNull { it.chapterNumber } ?: 0) + 1
            val chapter = NovelChapter(
                novelId = novelId,
                chapterNumber = nextNumber,
                title = title,
                outline = outline
            )
            novelRepository.insertChapter(chapter)
            refreshNovel()
        }
    }

    fun deleteChapter(chapter: NovelChapter) {
        viewModelScope.launch {
            novelRepository.deleteChapter(chapter)
            refreshNovel()
        }
    }

    fun updateChapterOutline(chapter: NovelChapter, newOutline: String) {
        viewModelScope.launch {
            novelRepository.updateChapter(chapter.copy(outline = newOutline, updatedAt = System.currentTimeMillis()))
        }
    }

    fun clearChapterContent(chapter: NovelChapter) {
        viewModelScope.launch {
            novelRepository.updateChapter(chapter.copy(content = "", updatedAt = System.currentTimeMillis()))
        }
    }

    private suspend fun refreshNovel() {
        _novel.value = novelRepository.getNovelById(novelId)
    }

    companion object {
        fun factory(
            novelRepository: NovelRepository,
            worldBookRepository: WorldBookRepository,
            characterRepository: CharacterRepository,
            novelId: Long
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NovelDetailViewModel(
                        novelRepository,
                        worldBookRepository,
                        characterRepository,
                        novelId
                    ) as T
                }
            }
        }
    }
}
