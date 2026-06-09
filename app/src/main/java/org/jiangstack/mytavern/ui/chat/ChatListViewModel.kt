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
import org.jiangstack.mytavern.domain.model.Novel
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.ChatRepository
import org.jiangstack.mytavern.domain.repository.NovelRepository
import org.jiangstack.mytavern.domain.repository.SessionCharacterRepository
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository
import org.jiangstack.mytavern.domain.repository.WorldBookRepository

class ChatListViewModel(
    private val chatRepository: ChatRepository,
    private val characterRepository: CharacterRepository,
    private val worldBookRepository: WorldBookRepository,
    private val novelRepository: NovelRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val sessionCharacterRepository: SessionCharacterRepository
) : ViewModel() {

    val sessions: StateFlow<List<ChatSession>> = chatRepository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiCharacters: StateFlow<List<Character>> = characterRepository.getAiCharacters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val worldBooks: StateFlow<List<org.jiangstack.mytavern.domain.model.WorldBook>> = worldBookRepository.getAllWorldBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val novels: StateFlow<List<Novel>> = novelRepository.getAllNovels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun createSession(aiCharacterId: Long, title: String, worldBookId: Long? = null): Long {
        val userCharacterId = userPreferencesRepository.defaultUserCharacterId
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
            .value

        val session = ChatSession(
            type = SessionType.SINGLE,
            title = title,
            aiCharacterId = aiCharacterId,
            userCharacterId = userCharacterId,
            worldBookId = worldBookId
        )
        return chatRepository.insertSession(session)
    }

    suspend fun createGroupSession(
        aiCharacterIds: List<Long>,
        title: String,
        worldBookId: Long? = null
    ): Long {
        val userCharacterId = userPreferencesRepository.defaultUserCharacterId
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
            .value

        val session = ChatSession(
            type = SessionType.GROUP,
            title = title,
            aiCharacterId = null,
            userCharacterId = userCharacterId,
            worldBookId = worldBookId
        )
        val sessionId = chatRepository.insertSession(session)
        sessionCharacterRepository.addCharactersToSession(sessionId, aiCharacterIds)
        return sessionId
    }

    suspend fun createAgentSession(
        novelId: Long,
        title: String,
        agentSystemPrompt: String? = null
    ): Long {
        val session = ChatSession(
            type = SessionType.AGENT,
            title = title,
            novelId = novelId,
            agentSystemPrompt = agentSystemPrompt
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
            worldBookRepository: WorldBookRepository,
            novelRepository: NovelRepository,
            userPreferencesRepository: UserPreferencesRepository,
            sessionCharacterRepository: SessionCharacterRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChatListViewModel(
                        chatRepository,
                        characterRepository,
                        worldBookRepository,
                        novelRepository,
                        userPreferencesRepository,
                        sessionCharacterRepository
                    ) as T
                }
            }
        }
    }
}
