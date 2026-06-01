package org.jiangstack.mytavern.ui.novel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.Novel
import org.jiangstack.mytavern.domain.model.NovelChapter
import org.jiangstack.mytavern.domain.model.WorldBook
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.NovelRepository
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository
import org.jiangstack.mytavern.domain.repository.WorldBookRepository
import org.jiangstack.mytavern.domain.service.LlmService

class NovelChapterEditViewModel(
    private val novelRepository: NovelRepository,
    private val worldBookRepository: WorldBookRepository,
    private val characterRepository: CharacterRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val llmService: LlmService,
    private val novelId: Long,
    private val chapterId: Long
) : ViewModel() {

    private val _chapter = MutableStateFlow<NovelChapter?>(null)
    val chapter: StateFlow<NovelChapter?> = _chapter

    private val _novel = MutableStateFlow<Novel?>(null)
    val novel: StateFlow<Novel?> = _novel

    private val _worldBook = MutableStateFlow<WorldBook?>(null)
    val worldBook: StateFlow<WorldBook?> = _worldBook

    private val _characters = MutableStateFlow<List<Character>>(emptyList())
    val characters: StateFlow<List<Character>> = _characters

    private val _allChapters = MutableStateFlow<List<NovelChapter>>(emptyList())

    // 编辑中的正文内容
    private val _editContent = MutableStateFlow("")
    val editContent: StateFlow<String> = _editContent

    // AI 续写状态
    private val _aiStreamingContent = MutableStateFlow("")
    val aiStreamingContent: StateFlow<String> = _aiStreamingContent

    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating: StateFlow<Boolean> = _isAiGenerating

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var aiJob: Job? = null
    private var autoSaveJob: Job? = null

    init {
        viewModelScope.launch {
            val chapter = novelRepository.getChapterById(chapterId)
            _chapter.value = chapter
            _editContent.value = chapter?.content ?: ""

            val novel = novelRepository.getNovelById(novelId)
            _novel.value = novel

            novel?.worldBookId?.let { wbId ->
                _worldBook.value = worldBookRepository.getWorldBookById(wbId)
            }

            val chars = novel?.characterIds?.mapNotNull {
                characterRepository.getCharacterById(it)
            } ?: emptyList()
            _characters.value = chars

            _allChapters.value = novelRepository.getChaptersByNovelIdSync(novelId)
        }
    }

    fun updateContent(newContent: String) {
        _editContent.value = newContent
        // 防抖自动保存
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1000)
            saveChapter()
        }
    }

    fun saveChapter() {
        viewModelScope.launch {
            val current = _chapter.value ?: return@launch
            novelRepository.updateChapter(
                current.copy(
                    content = _editContent.value,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateOutline(newOutline: String) {
        viewModelScope.launch {
            val current = _chapter.value ?: return@launch
            novelRepository.updateChapter(
                current.copy(
                    outline = newOutline,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _chapter.value = current.copy(outline = newOutline)
        }
    }

    fun startAiContinue(customRequest: String) {
        if (_isAiGenerating.value) return
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _isAiGenerating.value = true
            _aiStreamingContent.value = ""
            _errorMessage.value = null

            try {
                val systemPrompt = buildNovelPrompt(customRequest)
                val temperature = userPreferencesRepository.temperature.first()
                val maxTokens = userPreferencesRepository.maxTokens.first()

                // 构造一条消息让 AI 续写
                val promptMessage = org.jiangstack.mytavern.domain.model.ChatMessage(
                    sessionId = 0,
                    content = "请续写小说正文。直接输出续写内容，不要重复前文，不要加任何解释。",
                    role = "user"
                )

                val fullContent = StringBuilder()

                llmService.sendChatMessageStream(
                    messages = listOf(promptMessage),
                    systemPrompt = systemPrompt,
                    temperature = temperature,
                    maxTokens = maxTokens,
                    thinkingEnabled = false
                ).collect { chunk ->
                    if (chunk.content.isNotBlank()) {
                        fullContent.append(chunk.content)
                        _aiStreamingContent.value = fullContent.toString()
                    }
                }
            } catch (e: CancellationException) {
                // 用户取消
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "AI 续写失败"
            } finally {
                _isAiGenerating.value = false
            }
        }
    }

    fun cancelAiContinue() {
        aiJob?.cancel()
        aiJob = null
        _isAiGenerating.value = false
    }

    fun acceptAiContent() {
        val aiContent = _aiStreamingContent.value
        if (aiContent.isNotBlank()) {
            val currentContent = _editContent.value
            _editContent.value = if (currentContent.isNotBlank()) {
                currentContent + "\n\n" + aiContent
            } else {
                aiContent
            }
            _aiStreamingContent.value = ""
            saveChapter()
        }
    }

    fun discardAiContent() {
        _aiStreamingContent.value = ""
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private suspend fun buildNovelPrompt(customRequest: String): String {
        val novel = _novel.value ?: return ""
        val wb = _worldBook.value
        val chars = _characters.value
        val allChapters = _allChapters.value
        val currentChapter = _chapter.value
        val currentContent = _editContent.value

        return buildString {
            appendLine("你是一位小说创作助手，请根据以下信息续写小说。")
            appendLine()
            appendLine("小说名称：${novel.title}")
            if (novel.description.isNotBlank()) {
                appendLine("小说设定：${novel.description}")
            }

            if (wb != null) {
                appendLine()
                appendLine("世界书：${wb.name}")
                appendLine(wb.description)
                wb.rules.forEach { rule ->
                    appendLine("- ${rule.name}: ${rule.description}")
                }
            }

            if (chars.isNotEmpty()) {
                appendLine()
                appendLine("参与角色：")
                chars.forEach { char ->
                    appendLine("- ${char.name}: ${char.description}")
                }
            }

            if (allChapters.isNotEmpty()) {
                appendLine()
                appendLine("历史章节纲要：")
                allChapters.forEach { ch ->
                    val marker = if (ch.id == chapterId) "（当前章节）" else ""
                    appendLine("第${ch.chapterNumber}章 ${ch.title}$marker: ${ch.outline.ifBlank { "（无纲要）" }}")
                }
            }

            if (currentChapter != null) {
                appendLine()
                appendLine("当前章节：第${currentChapter.chapterNumber}章 ${currentChapter.title}")
                if (currentChapter.outline.isNotBlank()) {
                    appendLine("当前章节纲要：${currentChapter.outline}")
                }
            }

            if (currentContent.isNotBlank()) {
                appendLine()
                appendLine("已有正文（前文）：")
                // 取最后 2000 字作为上下文
                val tailContent = if (currentContent.length > 2000) {
                    "..." + currentContent.takeLast(2000)
                } else {
                    currentContent
                }
                appendLine(tailContent)
            }

            if (customRequest.isNotBlank()) {
                appendLine()
                appendLine("用户附加要求：$customRequest")
            }

            appendLine()
            appendLine("请续写小说正文，保持风格一致，承接前文情节。直接输出续写内容，不要重复前文，不要加任何解释或标题。")
        }
    }

    companion object {
        fun factory(
            novelRepository: NovelRepository,
            worldBookRepository: WorldBookRepository,
            characterRepository: CharacterRepository,
            userPreferencesRepository: UserPreferencesRepository,
            llmService: LlmService,
            novelId: Long,
            chapterId: Long
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NovelChapterEditViewModel(
                        novelRepository,
                        worldBookRepository,
                        characterRepository,
                        userPreferencesRepository,
                        llmService,
                        novelId,
                        chapterId
                    ) as T
                }
            }
        }
    }
}
