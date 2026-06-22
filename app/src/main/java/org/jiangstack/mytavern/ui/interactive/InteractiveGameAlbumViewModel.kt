package org.jiangstack.mytavern.ui.interactive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.InteractiveGame
import org.jiangstack.mytavern.domain.model.InteractiveGameImage
import org.jiangstack.mytavern.domain.repository.InteractiveGameImageRepository
import org.jiangstack.mytavern.domain.repository.InteractiveGameRepository
import java.io.File

class InteractiveGameAlbumViewModel(
    private val gameId: Long,
    private val gameRepository: InteractiveGameRepository,
    private val imageRepository: InteractiveGameImageRepository
) : ViewModel() {

    private val _game = MutableStateFlow<InteractiveGame?>(null)
    val game: StateFlow<InteractiveGame?> = _game.asStateFlow()

    private val _images = MutableStateFlow<List<InteractiveGameImage>>(emptyList())
    val images: StateFlow<List<InteractiveGameImage>> = _images.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _game.value = gameRepository.getGameById(gameId)
        }
        viewModelScope.launch {
            imageRepository.getImagesByGameId(gameId).collect {
                _images.value = it
            }
        }
    }

    fun deleteImage(image: InteractiveGameImage) {
        viewModelScope.launch {
            image.localUri?.let { uri ->
                try {
                    File(java.net.URI(uri)).delete()
                } catch (_: Exception) {}
            }
            imageRepository.deleteImage(image)
        }
    }

    fun setAsBackground(image: InteractiveGameImage) {
        viewModelScope.launch {
            val currentGame = _game.value ?: return@launch
            val updatedGame = currentGame.copy(backgroundImageUri = image.localUri)
            gameRepository.updateGame(updatedGame)
            _game.value = updatedGame
            _message.value = "已设为背景"
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    companion object {
        fun factory(
            gameId: Long,
            gameRepository: InteractiveGameRepository,
            imageRepository: InteractiveGameImageRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return InteractiveGameAlbumViewModel(
                        gameId,
                        gameRepository,
                        imageRepository
                    ) as T
                }
            }
        }
    }
}
