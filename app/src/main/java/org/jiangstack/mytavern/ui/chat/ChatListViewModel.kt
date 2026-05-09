package org.jiangstack.mytavern.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.jiangstack.mytavern.domain.repository.ChatRepository

class ChatListViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {
    val sessions = chatRepository.getAllSessions()

    companion object {
        fun factory(repository: ChatRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChatListViewModel(repository) as T
                }
            }
        }
    }
}
