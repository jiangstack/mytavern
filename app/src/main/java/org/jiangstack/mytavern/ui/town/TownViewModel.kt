package org.jiangstack.mytavern.ui.town

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.Town
import org.jiangstack.mytavern.domain.model.TownLocation
import org.jiangstack.mytavern.domain.model.TownMember
import org.jiangstack.mytavern.domain.model.TownRelationship
import org.jiangstack.mytavern.domain.model.TownScene
import org.jiangstack.mytavern.domain.model.TownSnapshot
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.TownRepository
import org.jiangstack.mytavern.domain.service.TownSimulationService

/** 居民动态卡片展示数据 */
data class TownMemberDisplay(
    val member: TownMember,
    val name: String,
    val avatarUri: String?,
    val locationName: String
)

/** 关系展示数据 */
data class TownRelationshipDisplay(
    val memberAId: Long,
    val memberBId: Long,
    val nameA: String,
    val nameB: String,
    val affinity: Int,
    val note: String
)

class TownViewModel(
    private val townId: Long,
    private val townRepository: TownRepository,
    characterRepository: CharacterRepository,
    private val townSimulationService: TownSimulationService
) : ViewModel() {

    val town: StateFlow<Town?> = townRepository.getTownByIdFlow(townId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val members: StateFlow<List<TownMember>> = townRepository.getMembersByTownId(townId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val locations: StateFlow<List<TownLocation>> = townRepository.getLocationsByTownId(townId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scenes: StateFlow<List<TownScene>> = townRepository.getScenesByTownId(townId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val relationships: StateFlow<List<TownRelationship>> = townRepository.getRelationshipsByTownId(townId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs = townRepository.getLogsByTownId(townId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val snapshots: StateFlow<List<TownSnapshot>> = townRepository.getSnapshotsByTownId(townId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val characters = characterRepository.getAllCharacters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memberDisplays: StateFlow<List<TownMemberDisplay>> =
        combine(members, characters, locations) { ms, cs, locs ->
            val charById = cs.associateBy { it.id }
            val locNameById = locs.associate { it.id to it.name }
            ms.map { m ->
                val c = charById[m.characterId]
                TownMemberDisplay(
                    member = m,
                    name = c?.name ?: "未知角色",
                    avatarUri = c?.avatarUri,
                    locationName = m.currentLocationId?.let { locNameById[it] } ?: ""
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val relationshipDisplays: StateFlow<List<TownRelationshipDisplay>> =
        combine(relationships, members, characters) { rels, ms, cs ->
            val nameByMemberId = ms.associate { m ->
                m.id to (cs.firstOrNull { it.id == m.characterId }?.name ?: "未知角色")
            }
            rels.map { r ->
                TownRelationshipDisplay(
                    memberAId = r.memberAId,
                    memberBId = r.memberBId,
                    nameA = nameByMemberId[r.memberAId] ?: "?",
                    nameB = nameByMemberId[r.memberBId] ?: "?",
                    affinity = r.affinity,
                    note = r.note
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _advancing = MutableStateFlow(false)
    val advancing: StateFlow<Boolean> = _advancing.asStateFlow()

    private val _progressHours = MutableStateFlow<Int?>(null)
    val progressHours: StateFlow<Int?> = _progressHours.asStateFlow()

    private val _injectingEvent = MutableStateFlow(false)
    val injectingEvent: StateFlow<Boolean> = _injectingEvent.asStateFlow()

    private val _snapshotBusy = MutableStateFlow(false)
    val snapshotBusy: StateFlow<Boolean> = _snapshotBusy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** 待查看的场景（一次性导航事件） */
    private val _pendingSceneId = MutableStateFlow<Long?>(null)
    val pendingSceneId: StateFlow<Long?> = _pendingSceneId.asStateFlow()

    fun clearError() { _error.value = null }
    fun consumePendingScene() { _pendingSceneId.value = null }

    fun advanceOneHour() {
        if (_advancing.value) return
        viewModelScope.launch {
            _advancing.value = true
            try {
                when (val result = townSimulationService.advanceOneHour(townId)) {
                    is TownSimulationService.AdvanceResult.Quiet -> Unit
                    is TownSimulationService.AdvanceResult.SceneCreated -> _pendingSceneId.value = result.sceneId
                    is TownSimulationService.AdvanceResult.InteractionPending -> _pendingSceneId.value = result.sceneId
                    is TownSimulationService.AdvanceResult.Failed -> _error.value = result.message
                }
            } finally {
                _advancing.value = false
            }
        }
    }

    fun advanceToNextEvent() {
        if (_advancing.value) return
        viewModelScope.launch {
            _advancing.value = true
            try {
                val result = townSimulationService.advanceToNextEvent(townId) { hours ->
                    _progressHours.value = hours
                }
                when (result) {
                    is TownSimulationService.AdvanceResult.Quiet -> Unit
                    is TownSimulationService.AdvanceResult.SceneCreated -> _pendingSceneId.value = result.sceneId
                    is TownSimulationService.AdvanceResult.InteractionPending -> _pendingSceneId.value = result.sceneId
                    is TownSimulationService.AdvanceResult.Failed -> _error.value = result.message
                }
            } finally {
                _advancing.value = false
                _progressHours.value = null
            }
        }
    }

    fun movePlayerTo(locationId: Long) {
        viewModelScope.launch {
            val town = town.value ?: return@launch
            val playerId = town.playMemberId ?: return@launch
            val player = townRepository.getMemberById(playerId) ?: return@launch
            townRepository.updateMember(player.copy(currentLocationId = locationId))
            townSimulationService.tryStartInteractionAtPlayerLocation(townId)
                .onSuccess { sceneId -> if (sceneId != null) _pendingSceneId.value = sceneId }
                .onFailure { _error.value = it.message }
        }
    }

    fun injectWorldEvent(brief: String) {
        if (brief.isBlank() || _injectingEvent.value) return
        viewModelScope.launch {
            _injectingEvent.value = true
            try {
                townSimulationService.injectWorldEvent(townId, brief.trim())
                    .onSuccess { sceneId -> _pendingSceneId.value = sceneId }
                    .onFailure { _error.value = it.message }
            } finally {
                _injectingEvent.value = false
            }
        }
    }

    fun saveSnapshot(name: String, onSaved: () -> Unit) {
        if (_snapshotBusy.value) return
        viewModelScope.launch {
            _snapshotBusy.value = true
            try {
                townSimulationService.createSnapshot(townId, name.ifBlank { "第${town.value?.currentDay}天存档" })
                    .onSuccess { onSaved() }
                    .onFailure { _error.value = it.message }
            } finally {
                _snapshotBusy.value = false
            }
        }
    }

    fun restoreSnapshot(snapshotId: Long, onRestored: () -> Unit) {
        if (_snapshotBusy.value) return
        viewModelScope.launch {
            _snapshotBusy.value = true
            try {
                townSimulationService.restoreSnapshot(snapshotId)
                    .onSuccess { onRestored() }
                    .onFailure { _error.value = it.message }
            } finally {
                _snapshotBusy.value = false
            }
        }
    }

    fun deleteSnapshot(snapshotId: Long) {
        viewModelScope.launch {
            townSimulationService.deleteSnapshot(snapshotId)
                .onFailure { _error.value = it.message }
        }
    }

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
                    return TownViewModel(townId, townRepository, characterRepository, townSimulationService) as T
                }
            }
        }
    }
}
