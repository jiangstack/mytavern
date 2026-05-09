package org.jiangstack.mytavern.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.jiangstack.mytavern.domain.repository.CharacterRepository

class CharacterListViewModel(
    private val characterRepository: CharacterRepository
) : ViewModel() {
    val characters = characterRepository.getAllCharacters()

    companion object {
        fun factory(repository: CharacterRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CharacterListViewModel(repository) as T
                }
            }
        }
    }
}
