package org.jiangstack.mytavern.ui.town

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.Town
import org.jiangstack.mytavern.domain.model.TownLocation
import org.jiangstack.mytavern.domain.model.TownMember
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.TownRepository
import org.jiangstack.mytavern.domain.service.TownSimulationService

/** 地点编辑草稿。id <= 0 表示新地点。 */
data class TownLocationDraft(
    val id: Long = 0,
    val name: String = "",
    val description: String = ""
)

class TownEditViewModel(
    private val townId: Long,
    private val townRepository: TownRepository,
    characterRepository: CharacterRepository,
    private val townSimulationService: TownSimulationService
) : ViewModel() {

    private val _town = MutableStateFlow<Town?>(null)
    val town: StateFlow<Town?> = _town.asStateFlow()

    val aiCharacters: StateFlow<List<Character>> = characterRepository.getAiCharacters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val name = MutableStateFlow("")
    val worldDescription = MutableStateFlow("")
    val selectedCharacterIds = MutableStateFlow<Set<Long>>(emptySet())
    val personaByCharacterId = MutableStateFlow<Map<Long, String>>(emptyMap())
    val playCharacterId = MutableStateFlow(0L)
    val locationDrafts = MutableStateFlow<List<TownLocationDraft>>(emptyList())
    val saving = MutableStateFlow(false)

    private var originalLocationIds: Set<Long> = emptySet()
    private var loaded = false

    init {
        viewModelScope.launch {
            val town = townRepository.getTownById(townId) ?: return@launch
            _town.value = town
            name.value = town.name
            worldDescription.value = town.worldDescription

            val members = townRepository.getMembersByTownIdSync(townId)
            val locations = townRepository.getLocationsByTownIdSync(townId)

            selectedCharacterIds.value = members.map { it.characterId }.toSet()
            personaByCharacterId.value = members.associate { it.characterId to it.persona }
            playCharacterId.value =
                members.firstOrNull { it.id == town.playMemberId }?.characterId ?: 0L

            originalLocationIds = locations.map { it.id }.toSet()
            locationDrafts.value = if (locations.isEmpty()) {
                defaultLocationDrafts()
            } else {
                locations.map { TownLocationDraft(it.id, it.name, it.description) }
            }
            loaded = true
        }
    }

    fun isLoaded(): Boolean = loaded

    fun toggleCharacter(character: Character) {
        val current = selectedCharacterIds.value.toMutableSet()
        if (character.id in current) {
            current.remove(character.id)
            if (playCharacterId.value == character.id) playCharacterId.value = 0L
        } else {
            current.add(character.id)
            if (personaByCharacterId.value[character.id].isNullOrBlank()) {
                personaByCharacterId.value =
                    personaByCharacterId.value + (character.id to character.description)
            }
        }
        selectedCharacterIds.value = current
    }

    fun updatePersona(characterId: Long, persona: String) {
        personaByCharacterId.value = personaByCharacterId.value + (characterId to persona)
    }

    fun addLocation() {
        locationDrafts.value = locationDrafts.value + TownLocationDraft()
    }

    fun updateLocation(index: Int, draft: TownLocationDraft) {
        if (index !in locationDrafts.value.indices) return
        locationDrafts.value = locationDrafts.value.toMutableList().apply { set(index, draft) }
    }

    fun removeLocation(index: Int) {
        if (index !in locationDrafts.value.indices) return
        locationDrafts.value = locationDrafts.value.toMutableList().apply { removeAt(index) }
    }

    fun save(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val town = _town.value ?: return
        viewModelScope.launch {
            saving.value = true
            try {
                // 同步地点
                val drafts = locationDrafts.value.filter { it.name.isNotBlank() }
                val keptIds = mutableSetOf<Long>()
                for (draft in drafts) {
                    if (draft.id > 0) {
                        townRepository.updateLocation(
                            TownLocation(id = draft.id, townId = townId, name = draft.name.trim(), description = draft.description.trim())
                        )
                        keptIds += draft.id
                    } else {
                        keptIds += townRepository.insertLocation(
                            TownLocation(townId = townId, name = draft.name.trim(), description = draft.description.trim())
                        )
                    }
                }
                originalLocationIds.filter { it !in keptIds }.forEach { id ->
                    townRepository.getLocationsByTownIdSync(townId)
                        .firstOrNull { it.id == id }
                        ?.let { townRepository.deleteLocation(it) }
                }

                // 同步成员
                val desired = selectedCharacterIds.value.map { cid ->
                    TownMember(
                        townId = townId,
                        characterId = cid,
                        persona = personaByCharacterId.value[cid] ?: "",
                        isPlayerControlled = cid == playCharacterId.value
                    )
                }
                val charToMember = townRepository.syncMembers(townId, desired)
                val playMemberId = if (playCharacterId.value != 0L) charToMember[playCharacterId.value] else null

                townRepository.updateTown(
                    town.copy(
                        name = name.value.trim(),
                        worldDescription = worldDescription.value.trim(),
                        playMemberId = playMemberId
                    )
                )

                // 行动表为空时生成（已有则跳过）
                townSimulationService.ensureSchedules(townId)
                    .onFailure { e -> onError(e.message ?: "行动表生成失败") }
                    .onSuccess { onSuccess() }
            } catch (e: Exception) {
                onError(e.message ?: "保存失败")
            } finally {
                saving.value = false
            }
        }
    }

    private fun defaultLocationDrafts(): List<TownLocationDraft> = listOf(
        TownLocationDraft(0, "中央广场", "小镇的心脏，居民们在此聚集、交谈、举办活动"),
        TownLocationDraft(0, "酒馆", "热闹的酒馆，消息和小道新闻的集散地"),
        TownLocationDraft(0, "集市", "摊位林立，柴米油盐与人情往来"),
        TownLocationDraft(0, "田地", "镇外的农田，日出而作日落而息"),
        TownLocationDraft(0, "铁匠铺", "叮当的打铁声与炉火"),
        TownLocationDraft(0, "民居", "居民们的家，夜晚归宿")
    )

    companion object {
        fun factory(
            townId: Long,
            townRepository: TownRepository,
            characterRepository: CharacterRepository,
            townSimulationService: TownSimulationService
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TownEditViewModel(townId, townRepository, characterRepository, townSimulationService) as T
                }
            }
        }
    }
}
