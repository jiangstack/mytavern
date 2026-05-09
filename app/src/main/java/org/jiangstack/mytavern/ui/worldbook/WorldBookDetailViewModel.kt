package org.jiangstack.mytavern.ui.worldbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.WorldBook
import org.jiangstack.mytavern.domain.model.WorldBookRule
import org.jiangstack.mytavern.domain.repository.WorldBookRepository

class WorldBookDetailViewModel(
    private val worldBookRepository: WorldBookRepository,
    private val worldBookId: Long
) : ViewModel() {

    val worldBook: StateFlow<WorldBook?> = flow {
        emit(worldBookRepository.getWorldBookById(worldBookId))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val rules: StateFlow<List<WorldBookRule>> = worldBookRepository.getRulesByWorldBookId(worldBookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveRule(rule: WorldBookRule) {
        viewModelScope.launch {
            if (rule.id == 0L) {
                worldBookRepository.insertRule(rule)
            } else {
                worldBookRepository.updateRule(rule)
            }
        }
    }

    fun deleteRule(rule: WorldBookRule) {
        viewModelScope.launch {
            worldBookRepository.deleteRule(rule)
        }
    }

    companion object {
        fun factory(
            repository: WorldBookRepository,
            worldBookId: Long
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WorldBookDetailViewModel(repository, worldBookId) as T
                }
            }
        }
    }
}
