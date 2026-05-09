package org.jiangstack.mytavern.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.ChatSession
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.SessionType
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.ChatRepository
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository

class ChatListViewModel(
    private val chatRepository: ChatRepository,
    private val characterRepository: CharacterRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val sessions: StateFlow<List<ChatSession>> = chatRepository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiCharacters: StateFlow<List<Character>> = characterRepository.getAiCharacters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun createSession(aiCharacterId: Long, title: String): Long {
        val userCharacterId = userPreferencesRepository.defaultUserCharacterId
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
            .value

        val session = ChatSession(
            type = SessionType.SINGLE,
            title = title,
            aiCharacterId = aiCharacterId,
            userCharacterId = userCharacterId
        )
        return chatRepository.insertSession(session)
    }

    fun deleteSession(session: ChatSession) {
        viewModelScope.launch {
            chatRepository.deleteMessagesBySessionId(session.id)
            chatRepository.deleteSession(session)
        }
    }

    companion object {
        fun factory(
            chatRepository: ChatRepository,
            characterRepository: CharacterRepository,
            userPreferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChatListViewModel(
                        chatRepository,
                        characterRepository,
                        userPreferencesRepository
                    ) as T
                }
            }
        }
    }
}
