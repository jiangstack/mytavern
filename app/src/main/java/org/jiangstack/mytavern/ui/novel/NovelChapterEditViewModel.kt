package org.jiangstack.mytavern.ui.novel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.Novel
import org.jiangstack.mytavern.domain.model.NovelChapter
import org.jiangstack.mytavern.domain.model.NovelCharacterItem
import org.jiangstack.mytavern.domain.model.PromptBlockConfig
import org.jiangstack.mytavern.domain.model.PromptBlockDefaults
import org.jiangstack.mytavern.domain.model.PromptBlockType
import org.jiangstack.mytavern.domain.model.WorldBook
import org.jiangstack.mytavern.domain.repository.CharacterRepository
import org.jiangstack.mytavern.domain.repository.NovelRepository
import org.jiangstack.mytavern.domain.repository.UserPreferencesRepository
import org.jiangstack.mytavern.domain.repository.WorldBookRepository
import org.jiangstack.mytavern.domain.service.LlmService
import org.jiangstack.mytavern.domain.service.UsageStatsTracker

fun buildDialogueAnnotatedString(text: String, dialogueColor: Color): androidx.compose.ui.text.AnnotatedString {
    val regex = Regex("([\u300c\u300e\u201c\u2018])(.*?)([\u300d\u300f\u201d\u2019])")
    return buildAnnotatedString {
        var lastEnd = 0
        for (match in regex.findAll(text)) {
            append(text.substring(lastEnd, match.range.first))
            withStyle(SpanStyle(color = dialogueColor)) {
                append(match.value)
            }
            lastEnd = match.range.last + 1
        }
        if (lastEnd < text.length) {
            append(text.substring(lastEnd))
        }
    }
}

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
    private val _characterItems = MutableStateFlow<List<NovelCharacterItem>>(emptyList())
    val characterItems: StateFlow<List<NovelCharacterItem>> = _characterItems

    private val _allChapters = MutableStateFlow<List<NovelChapter>>(emptyList())

    // 编辑中的正文内容
    private val _editContent = MutableStateFlow("")
    val editContent: StateFlow<String> = _editContent

    // AI 续写状态
    private val _aiStreamingContent = MutableStateFlow("")
    val aiStreamingContent: StateFlow<String> = _aiStreamingContent

    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating: StateFlow<Boolean> = _isAiGenerating

    // AI 纲要总结状态
    private val _outlineSummary = MutableStateFlow("")
    val outlineSummary: StateFlow<String> = _outlineSummary

    private val _isSummarizingOutline = MutableStateFlow(false)
    val isSummarizingOutline: StateFlow<Boolean> = _isSummarizingOutline

    // AI 修改状态
    private val _aiModifyContent = MutableStateFlow("")
    val aiModifyContent: StateFlow<String> = _aiModifyContent

    private val _isAiModifying = MutableStateFlow(false)
    val isAiModifying: StateFlow<Boolean> = _isAiModifying

    // 记录 AI 修改时的选区范围，用于采纳时替换
    private var aiModifyTargetRange: IntRange? = null

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    val dialogueHighlightEnabled: StateFlow<Boolean> = userPreferencesRepository.dialogueHighlightEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dialogueHighlightColor: StateFlow<Long> = userPreferencesRepository.dialogueHighlightColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF4FC3F7L)

    // 缓存上次 AI 续写的额外要求（内存暂存，关闭应用后失效）
    var cachedCustomRequest: String = ""
        private set

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

            _characterItems.value = novelRepository.getCharacterItemsByNovelIdSync(novelId)

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
            val updated = current.copy(
                content = _editContent.value,
                updatedAt = System.currentTimeMillis()
            )
            novelRepository.updateChapter(updated)
            _chapter.value = updated
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
        cachedCustomRequest = customRequest
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
                var finalUsage: org.jiangstack.mytavern.data.remote.Usage? = null

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
                    if (chunk.usage != null) {
                        finalUsage = chunk.usage
                    }
                }

                finalUsage?.let { UsageStatsTracker.recordUsage(it) }
            } catch (e: CancellationException) {
                // 用户取消
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "AI 续写失败"
            } finally {
                _isAiGenerating.value = false
            }
        }
    }

    fun cancelAiOperation() {
        aiJob?.cancel()
        aiJob = null
        _isAiGenerating.value = false
        _isAiModifying.value = false
        _isSummarizingOutline.value = false
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

    fun summarizeOutline() {
        val content = _editContent.value
        if (content.isBlank() || _isSummarizingOutline.value) return
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _isSummarizingOutline.value = true
            _outlineSummary.value = ""
            _errorMessage.value = null

            try {
                val temperature = userPreferencesRepository.temperature.first()
                val maxTokens = userPreferencesRepository.maxTokens.first()

                val systemPrompt = buildOutlinePrompt()

                val promptMessage = org.jiangstack.mytavern.domain.model.ChatMessage(
                    sessionId = 0,
                    content = "请总结本章纲要。",
                    role = "user"
                )

                val fullContent = StringBuilder()
                var finalUsage: org.jiangstack.mytavern.data.remote.Usage? = null

                llmService.sendChatMessageStream(
                    messages = listOf(promptMessage),
                    systemPrompt = systemPrompt,
                    temperature = temperature,
                    maxTokens = 512,
                    thinkingEnabled = false
                ).collect { chunk ->
                    if (chunk.content.isNotBlank()) {
                        fullContent.append(chunk.content)
                        _outlineSummary.value = fullContent.toString()
                    }
                    if (chunk.usage != null) {
                        finalUsage = chunk.usage
                    }
                }

                finalUsage?.let { UsageStatsTracker.recordUsage(it) }
            } catch (e: CancellationException) {
                // 用户取消
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "纲要总结失败"
            } finally {
                _isSummarizingOutline.value = false
            }
        }
    }

    fun acceptOutlineSummary() {
        val summary = _outlineSummary.value
        if (summary.isNotBlank()) {
            updateOutline(summary)
            _outlineSummary.value = ""
        }
    }

    fun discardOutlineSummary() {
        _outlineSummary.value = ""
    }

    fun startAiModify(selectedText: String, customRequest: String) {
        if (_isAiModifying.value) return
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _isAiModifying.value = true
            _aiModifyContent.value = ""
            _errorMessage.value = null

            try {
                val systemPrompt = buildNovelModifyPrompt(selectedText, customRequest)
                val temperature = userPreferencesRepository.temperature.first()
                val maxTokens = userPreferencesRepository.maxTokens.first()

                val promptMessage = org.jiangstack.mytavern.domain.model.ChatMessage(
                    sessionId = 0,
                    content = "请按照要求修改上述文本。直接输出修改后的内容。",
                    role = "user"
                )

                val fullContent = StringBuilder()
                var finalUsage: org.jiangstack.mytavern.data.remote.Usage? = null

                llmService.sendChatMessageStream(
                    messages = listOf(promptMessage),
                    systemPrompt = systemPrompt,
                    temperature = temperature,
                    maxTokens = maxTokens,
                    thinkingEnabled = false
                ).collect { chunk ->
                    if (chunk.content.isNotBlank()) {
                        fullContent.append(chunk.content)
                        _aiModifyContent.value = fullContent.toString()
                    }
                    if (chunk.usage != null) {
                        finalUsage = chunk.usage
                    }
                }

                finalUsage?.let { UsageStatsTracker.recordUsage(it) }
            } catch (e: CancellationException) {
                // 用户取消
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "AI 修改失败"
            } finally {
                _isAiModifying.value = false
            }
        }
    }

    fun setAiModifyTargetRange(range: IntRange?) {
        aiModifyTargetRange = range
    }

    fun acceptAiModify() {
        val modifyResult = _aiModifyContent.value
        if (modifyResult.isBlank()) return

        val currentContent = _editContent.value
        val range = aiModifyTargetRange

        _editContent.value = if (range != null && range.first < currentContent.length) {
            val end = minOf(range.last, currentContent.length - 1)
            currentContent.replaceRange(range.first, end + 1, modifyResult)
        } else {
            modifyResult
        }
        _aiModifyContent.value = ""
        aiModifyTargetRange = null
        saveChapter()
    }

    fun discardAiModify() {
        _aiModifyContent.value = ""
        aiModifyTargetRange = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private suspend fun buildNovelPrompt(customRequest: String): String {
        val blocks = userPreferencesRepository.novelPromptBlocks.first()
            .filter { it.isEnabled }
            .sortedBy { it.sortOrder }

        return blocks.mapNotNull { block ->
            val content = buildPromptBlockContent(block, customRequest, isContinue = true)
            content.takeIf { it.isNotBlank() }
        }.joinToString("\n\n")
    }

    private suspend fun buildNovelModifyPrompt(selectedText: String, customRequest: String): String {
        val blocks = userPreferencesRepository.novelModifyPromptBlocks.first()
            .filter { it.isEnabled }
            .sortedBy { it.sortOrder }

        return blocks.mapNotNull { block ->
            val content = buildPromptBlockContent(block, customRequest, isContinue = false, selectedText = selectedText)
            content.takeIf { it.isNotBlank() }
        }.joinToString("\n\n")
    }

    private fun buildPromptBlockContent(
        block: PromptBlockConfig,
        customRequest: String,
        isContinue: Boolean,
        selectedText: String = "",
        isOutline: Boolean = false
    ): String {
        val novel = _novel.value
        val wb = _worldBook.value
        val chars = _characters.value
        val charItems = _characterItems.value
        val allChapters = _allChapters.value
        val currentChapter = _chapter.value
        val currentContent = _editContent.value

        // 可编辑分块：使用自定义内容或默认内容
        if (block.type.editable) {
            val content = block.customContent
                ?: PromptBlockDefaults.defaultContent(block.type, isContinue, isOutline)
                ?: ""
            return when (block.type) {
                PromptBlockType.CUSTOM_REQUEST -> {
                    if (customRequest.isNotBlank()) content.replace("{customRequest}", customRequest) else ""
                }
                else -> content
            }
        }

        // 不可编辑分块：根据类型生成动态内容
        return when (block.type) {
            PromptBlockType.NOVEL_META -> {
                if (novel == null) return ""
                buildString {
                    appendLine("小说名称：${novel.title}")
                    if (novel.description.isNotBlank()) {
                        appendLine("小说设定：${novel.description}")
                    }
                }.trimEnd()
            }

            PromptBlockType.WORLD_BOOK -> {
                if (wb == null) return ""
                buildString {
                    appendLine("世界书：${wb.name}")
                    appendLine(wb.description)
                    wb.rules.forEach { rule ->
                        appendLine("- ${rule.name}: ${rule.description}")
                    }
                }.trimEnd()
            }

            PromptBlockType.CHARACTERS -> {
                if (chars.isEmpty()) return ""
                buildString {
                    appendLine("参与角色：")
                    chars.forEach { char ->
                        appendLine("- ${char.name}: ${char.description}")
                    }
                }.trimEnd()
            }

            PromptBlockType.CHARACTER_ITEMS -> {
                if (chars.isEmpty() || charItems.isEmpty()) return ""
                val itemsByCharacter = charItems.groupBy { it.characterId }
                buildString {
                    appendLine("人物特殊物品：")
                    chars.forEach { char ->
                        val items = itemsByCharacter[char.id].orEmpty()
                        if (items.isEmpty()) return@forEach
                        appendLine("- ${char.name}：")
                        items.forEach { item ->
                            if (item.description.isNotBlank()) {
                                appendLine("  · ${item.name}：${item.description}")
                            } else {
                                appendLine("  · ${item.name}")
                            }
                        }
                    }
                }.trimEnd()
            }

            PromptBlockType.CHAPTER_OUTLINES -> {
                if (allChapters.isEmpty()) return ""
                buildString {
                    if (isContinue) {
                        appendLine("历史章节纲要：")
                    } else {
                        appendLine("章节纲要：")
                    }
                    allChapters.forEach { ch ->
                        val marker = if (ch.id == chapterId) "（当前章节）" else ""
                        appendLine("第${ch.chapterNumber}章 ${ch.title}$marker: ${ch.outline.ifBlank { "（无纲要）" }}")
                    }
                }.trimEnd()
            }

            PromptBlockType.PREVIOUS_CHAPTER -> {
                if (currentChapter == null || currentChapter.chapterNumber <= 1) return ""
                val prevChapter = allChapters.find { it.chapterNumber == currentChapter.chapterNumber - 1 }
                if (prevChapter == null || prevChapter.content.isBlank()) return ""
                buildString {
                    appendLine("上一章正文（第${prevChapter.chapterNumber}章 ${prevChapter.title}）：")
                    val prevTailContent = if (prevChapter.content.length > 2000) {
                        "..." + prevChapter.content.takeLast(2000)
                    } else {
                        prevChapter.content
                    }
                    appendLine(prevTailContent)
                }.trimEnd()
            }

            PromptBlockType.CURRENT_CHAPTER -> {
                if (currentChapter == null) return ""
                buildString {
                    appendLine("当前章节：第${currentChapter.chapterNumber}章 ${currentChapter.title}")
                    if (currentChapter.outline.isNotBlank()) {
                        appendLine("当前章节纲要：${currentChapter.outline}")
                    }
                }.trimEnd()
            }

            PromptBlockType.EXISTING_CONTENT -> {
                if (currentContent.isBlank()) return ""
                buildString {
                    appendLine("已有正文（前文）：")
                    val tailContent = if (currentContent.length > 2000) {
                        "..." + currentContent.takeLast(2000)
                    } else {
                        currentContent
                    }
                    appendLine(tailContent)
                }.trimEnd()
            }

            PromptBlockType.SELECTED_TEXT -> {
                if (selectedText.isBlank()) return ""
                buildString {
                    appendLine("待修改的文本：")
                    appendLine(selectedText)
                }.trimEnd()
            }

            PromptBlockType.CHAPTER_CONTENT -> {
                if (currentContent.isBlank()) return ""
                buildString {
                    appendLine("章节正文：")
                    appendLine(currentContent)
                }.trimEnd()
            }

            else -> ""
        }
    }

    private suspend fun buildOutlinePrompt(): String {
        val blocks = userPreferencesRepository.novelOutlinePromptBlocks.first()
            .filter { it.isEnabled }
            .sortedBy { it.sortOrder }

        return blocks.mapNotNull { block ->
            val content = buildPromptBlockContent(block, customRequest = "", isContinue = false, isOutline = true)
            content.takeIf { it.isNotBlank() }
        }.joinToString("\n\n")
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
