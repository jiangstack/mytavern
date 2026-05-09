package org.jiangstack.mytavern.ui.worldbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.jiangstack.mytavern.domain.repository.WorldBookRepository

class WorldBookListViewModel(
    private val worldBookRepository: WorldBookRepository
) : ViewModel() {
    val worldBooks = worldBookRepository.getAllWorldBooks()

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
