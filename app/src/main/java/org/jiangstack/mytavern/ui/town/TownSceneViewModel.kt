package org.jiangstack.mytavern.ui.town

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.SceneLine
import org.jiangstack.mytavern.domain.model.SceneStatus
import org.jiangstack.mytavern.domain.model.Town
import org.jiangstack.mytavern.domain.model.TownScene
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.TownRepository
import org.jiangstack.mytavern.domain.service.TownSimulationService

class TownSceneViewModel(
    private val townId: Long,
    private val sceneId: Long,
    private val townRepository: TownRepository,
    private val characterRepository: CharacterRepository,
    private val townSimulationService: TownSimulationService
) : ViewModel() {

    data class ParticipantDisplay(
        val memberId: Long,
        val name: String,
        val avatarUri: String?
    )

    private val _scene = MutableStateFlow<TownScene?>(null)
    val scene: StateFlow<TownScene?> = _scene.asStateFlow()

    private val _town = MutableStateFlow<Town?>(null)
    val town: StateFlow<Town?> = _town.asStateFlow()

    private val _locationName = MutableStateFlow("")
    val locationName: StateFlow<String> = _locationName.asStateFlow()

    private val _participants = MutableStateFlow<List<ParticipantDisplay>>(emptyList())
    val participants: StateFlow<List<ParticipantDisplay>> = _participants.asStateFlow()

    private val _lines = MutableStateFlow<List<SceneLine>>(emptyList())
    val lines: StateFlow<List<SceneLine>> = _lines.asStateFlow()

    private val _streamingText = MutableStateFlow<String?>(null)
    val streamingText: StateFlow<String?> = _streamingText.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _isDone = MutableStateFlow(false)
    val isDone: StateFlow<Boolean> = _isDone.asStateFlow()

    private val _endBusy = MutableStateFlow(false)
    val endBusy: StateFlow<Boolean> = _endBusy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val scene = townRepository.getSceneById(sceneId) ?: run {
            _error.value = "场景不存在"
            return
        }
        _scene.value = scene
        _lines.value = scene.lines
        _town.value = townRepository.getTownById(townId)
        _locationName.value = townRepository.getLocationsByTownIdSync(townId)
            .firstOrNull { it.id == scene.locationId }?.name ?: ""

        val members = townRepository.getMembersByTownIdSync(townId).filter { it.id in scene.participantIds }
        val characters = characterRepository.getCharactersByIds(members.map { it.characterId })
        _participants.value = members.map { m ->
            val c = characters.firstOrNull { it.id == m.characterId }
            ParticipantDisplay(memberId = m.id, name = c?.name ?: "未知角色", avatarUri = c?.avatarUri)
        }

        when (scene.status) {
            SceneStatus.DONE -> {
                _isDone.value = true
            }
            SceneStatus.PENDING -> {
                _isDone.value = false
                startGeneration(finalStatus = SceneStatus.DONE, recordMemory = true, playerInput = null)
            }
            SceneStatus.AWAITING_PLAYER -> {
                _isDone.value = false
                startGeneration(finalStatus = SceneStatus.INTERACTIVE, recordMemory = false, playerInput = null)
            }
            // 后台生成中（生成不随页面退出中断）：观察数据库直至完成，再展示结果
            SceneStatus.GENERATING -> {
                _isDone.value = false
                awaitExternalGeneration()
            }
            else -> Unit
        }
    }

    private suspend fun awaitExternalGeneration() {
        _isStreaming.value = true
        try {
            val finished = townRepository.getSceneByIdFlow(sceneId)
                .first { s -> s != null && s.status != SceneStatus.GENERATING } ?: return
            _scene.value = finished
            _lines.value = finished.lines
            _isDone.value = finished.status == SceneStatus.DONE
        } finally {
            _isStreaming.value = false
        }
    }

    /**
     * 场景生成跑在服务的应用级作用域中：退出本页面后生成继续，完成后落库。
     * onEvent 仅在本页面存活期间驱动流式 UI。
     */
    private fun startGeneration(finalStatus: SceneStatus, recordMemory: Boolean, playerInput: String?) {
        if (_isStreaming.value) return
        _isStreaming.value = true
        _streamingText.value = null
        townSimulationService.generateSceneInBackground(
            sceneId = sceneId,
            finalStatus = finalStatus,
            recordMemory = recordMemory,
            playerInput = playerInput,
            useFinishTool = finalStatus != SceneStatus.INTERACTIVE // 互动场景由用户决定结束
        ) { event ->
            when (event) {
                is TownSimulationService.SceneEvent.LineDelta -> _streamingText.value = event.text
                is TownSimulationService.SceneEvent.LineAppended -> {
                    _lines.value = _lines.value + event.line
                    _streamingText.value = null
                }
                is TownSimulationService.SceneEvent.ToolResult -> Unit
                    is TownSimulationService.SceneEvent.Finished -> {
                        _scene.value = event.scene
                        _lines.value = event.scene.lines
                        _streamingText.value = null
                        _isDone.value = true
                    }
                is TownSimulationService.SceneEvent.Error -> _error.value = event.message
            }
        }.invokeOnCompletion {
            _isStreaming.value = false
            _streamingText.value = null
        }
    }

    fun updateInput(text: String) {
        _inputText.value = text
    }

    fun sendInput() {
        val text = _inputText.value.trim()
        val scene = _scene.value ?: return
        if (text.isBlank() || _isStreaming.value) return
        if (scene.status != SceneStatus.INTERACTIVE && scene.status != SceneStatus.AWAITING_PLAYER) return
        _inputText.value = ""
        startGeneration(finalStatus = SceneStatus.INTERACTIVE, recordMemory = false, playerInput = text)
    }

    fun endScene(onDone: () -> Unit) {
        if (_endBusy.value || _isStreaming.value) return
        viewModelScope.launch {
            _endBusy.value = true
            try {
                townSimulationService.endScene(sceneId)
                    .onSuccess { updated ->
                        _scene.value = updated
                        _lines.value = updated.lines
                        _isDone.value = true
                        onDone()
                    }
                    .onFailure { _error.value = it.message }
            } finally {
                _endBusy.value = false
            }
        }
    }

    fun forceStopScene() {
        if (_isStreaming.value) return
        viewModelScope.launch {
            _isStreaming.value = true
            try {
                townSimulationService.forceStopScene(sceneId)
                    .onSuccess { updated ->
                        _scene.value = updated
                        _lines.value = updated.lines
                        _isDone.value = true
                    }
                    .onFailure { _error.value = it.message }
            } finally {
                _isStreaming.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        fun factory(
            townId: Long,
            sceneId: Long,
            townRepository: TownRepository,
            characterRepository: CharacterRepository,
            townSimulationService: TownSimulationService
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TownSceneViewModel(
                        townId, sceneId, townRepository, characterRepository, townSimulationService
                    ) as T
                }
            }
        }
    }
}
