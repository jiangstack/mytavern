package org.jiangstack.mytavern.ui.town

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.Town
import org.jiangstack.mytavern.domain.repository.TownRepository

class TownListViewModel(
    private val townRepository: TownRepository
) : ViewModel() {

    val towns: StateFlow<List<Town>> = townRepository.getAllTowns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createTown(name: String, worldDescription: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = townRepository.insertTown(Town(name = name.trim(), worldDescription = worldDescription.trim()))
            onCreated(id)
        }
    }

    fun deleteTown(town: Town) {
        viewModelScope.launch {
            townRepository.deleteTown(town)
        }
    }

    companion object {
        fun factory(townRepository: TownRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TownListViewModel(townRepository) as T
                }
            }
        }
    }
}
