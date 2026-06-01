package org.jiangstack.mytavern.ui.worldbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.WorldBook
import org.jiangstack.mytavern.domain.repository.WorldBookRepository

class WorldBookListViewModel(
    private val worldBookRepository: WorldBookRepository
) : ViewModel() {
    val worldBooks = worldBookRepository.getAllWorldBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveWorldBook(worldBook: WorldBook) {
        viewModelScope.launch {
            if (worldBook.id == 0L) {
                worldBookRepository.insertWorldBook(worldBook)
            } else {
                worldBookRepository.updateWorldBook(worldBook)
            }
        }
    }

    fun deleteWorldBook(worldBook: WorldBook) {
        viewModelScope.launch {
            worldBookRepository.deleteWorldBook(worldBook)
        }
    }

    fun copyWorldBook(worldBookId: Long) {
        viewModelScope.launch {
            worldBookRepository.copyWorldBook(worldBookId)
        }
    }

    companion object {
        fun factory(repository: WorldBookRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WorldBookListViewModel(repository) as T
                }
            }
        }
    }
}
