package org.jiangstack.mytavern.ui.novel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jiangstack.mytavern.MyTavernApplication
import org.jiangstack.mytavern.R
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.NovelChapter
import org.jiangstack.mytavern.domain.model.WorldBook

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelDetailScreen(
    novelId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToChapterEdit: (Long, Long) -> Unit,
    onNavigateToCharacterItems: () -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: NovelDetailViewModel = viewModel(
        factory = NovelDetailViewModel.factory(
            container.novelRepository,
            container.worldBookRepository,
            container.characterRepository,
            novelId
        )
    )

    val novel by viewModel.novel.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val worldBook by viewModel.worldBook.collectAsState()
    val characters by viewModel.characters.collectAsState()
    val allWorldBooks by viewModel.allWorldBooks.collectAsState()
    val allAiCharacters by viewModel.allAiCharacters.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var showDeleteChapterDialog by remember { mutableStateOf(false) }
    var chapterToDelete by remember { mutableStateOf<NovelChapter?>(null) }
    var showOutlineDialog by remember { mutableStateOf(false) }
    var chapterForOutline by remember { mutableStateOf<NovelChapter?>(null) }
    var showClearContentDialog by remember { mutableStateOf(false) }
    var chapterToClear by remember { mutableStateOf<NovelChapter?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(novel?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCharacterItems) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.novel_character_items_title))
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.novel_edit_title))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddChapterDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.novel_chapter_add))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 小说信息卡片
            novel?.let { n ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (n.description.isNotBlank()) {
                            Text(
                                text = n.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (worldBook != null) {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    modifier = Modifier.height(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = worldBook!!.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            if (characters.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.height(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = characters.joinToString(", ") { it.name },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }


            // 章节标题
            Text(
                text = stringResource(R.string.novel_chapters_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (chapters.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.novel_chapter_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chapters, key = { it.id }) { chapter ->
                        ChapterItem(
                            chapter = chapter,
                            onClick = { onNavigateToChapterEdit(novelId, chapter.id) },
                            onLongClick = {
                                chapterToDelete = chapter
                                showDeleteChapterDialog = true
                            },
                            onEditOutline = {
                                chapterForOutline = chapter
                                showOutlineDialog = true
                            },
                            onClearContent = {
                                chapterToClear = chapter
                                showClearContentDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // 编辑小说对话框
    if (showEditDialog && novel != null) {
        NovelEditDialog(
            novel = novel!!,
            worldBooks = allWorldBooks,
            aiCharacters = allAiCharacters,
            onDismiss = { showEditDialog = false },
            onConfirm = { title, description, worldBookId, characterIds ->
                viewModel.updateNovel(title, description, worldBookId, characterIds)
                showEditDialog = false
            }
        )
    }

    // 添加章节对话框
    if (showAddChapterDialog) {
        ChapterAddDialog(
            onDismiss = { showAddChapterDialog = false },
            onConfirm = { title, outline ->
                viewModel.addChapter(title, outline)
                showAddChapterDialog = false
            }
        )
    }

    // 删除章节确认
    if (showDeleteChapterDialog && chapterToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteChapterDialog = false },
            title = { Text(stringResource(R.string.novel_chapter_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.novel_chapter_delete_message,
                        chapterToDelete!!.chapterNumber,
                        chapterToDelete!!.title
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteChapter(chapterToDelete!!)
                        showDeleteChapterDialog = false
                        chapterToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteChapterDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 编辑纲要对话框
    if (showOutlineDialog && chapterForOutline != null) {
        OutlineEditDialog(
            chapter = chapterForOutline!!,
            onDismiss = { showOutlineDialog = false },
            onConfirm = { newOutline ->
                viewModel.updateChapterOutline(chapterForOutline!!, newOutline)
                showOutlineDialog = false
            }
        )
    }

    // 清空章节内容确认
    if (showClearContentDialog && chapterToClear != null) {
        AlertDialog(
            onDismissRequest = { showClearContentDialog = false },
            title = { Text(stringResource(R.string.novel_chapter_clear_content_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.novel_chapter_clear_content_message,
                        chapterToClear!!.chapterNumber,
                        chapterToClear!!.title
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearChapterContent(chapterToClear!!)
                        showClearContentDialog = false
                        chapterToClear = null
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearContentDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChapterItem(
    chapter: NovelChapter,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEditOutline: () -> Unit,
    onClearContent: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "第${chapter.chapterNumber}章 ${chapter.title}",
                    style = MaterialTheme.typography.titleSmall
                )
                if (chapter.outline.isNotBlank()) {
                    Text(
                        text = chapter.outline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (chapter.content.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.novel_word_count, chapter.content.length),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            IconButton(onClick = onEditOutline) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.novel_chapter_outline),
                    modifier = Modifier.height(20.dp)
                )
            }
            if (chapter.content.isNotBlank()) {
                IconButton(onClick = onClearContent) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.novel_chapter_clear_content_title),
                        modifier = Modifier.height(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, outline: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var outline by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.novel_chapter_add)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.novel_chapter_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = outline,
                    onValueChange = { outline = it },
                    label = { Text(stringResource(R.string.novel_chapter_outline)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, outline) },
                enabled = title.isNotBlank()
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun OutlineEditDialog(
    chapter: NovelChapter,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var outline by remember { mutableStateOf(chapter.outline) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.novel_chapter_outline)) },
        text = {
            OutlinedTextField(
                value = outline,
                onValueChange = { outline = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(outline) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun NovelEditDialog(
    novel: org.jiangstack.mytavern.domain.model.Novel,
    worldBooks: List<WorldBook>,
    aiCharacters: List<Character>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, worldBookId: Long?, characterIds: List<Long>) -> Unit
) {
    var title by remember { mutableStateOf(novel.title) }
    var description by remember { mutableStateOf(novel.description) }
    var selectedWorldBookId by remember { mutableStateOf(novel.worldBookId) }
    var worldBookExpanded by remember { mutableStateOf(false) }
    val selectedCharacterIds = remember { mutableStateListOf(*novel.characterIds.toTypedArray()) }

    val selectedWorldBookName = worldBooks.find { it.id == selectedWorldBookId }?.name

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.novel_edit_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.novel_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.novel_description)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = worldBookExpanded,
                    onExpandedChange = { worldBookExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedWorldBookName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.novel_select_worldbook)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = worldBookExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = worldBookExpanded,
                        onDismissRequest = { worldBookExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.novel_worldbook_none)) },
                            onClick = {
                                selectedWorldBookId = null
                                worldBookExpanded = false
                            }
                        )
                        worldBooks.forEach { wb ->
                            DropdownMenuItem(
                                text = { Text(wb.name) },
                                onClick = {
                                    selectedWorldBookId = wb.id
                                    worldBookExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.novel_select_characters),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    aiCharacters.forEach { character ->
                        FilterChip(
                            selected = selectedCharacterIds.contains(character.id),
                            onClick = {
                                if (selectedCharacterIds.contains(character.id)) {
                                    selectedCharacterIds.remove(character.id)
                                } else {
                                    selectedCharacterIds.add(character.id)
                                }
                            },
                            label = { Text(character.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(title, description, selectedWorldBookId, selectedCharacterIds.toList())
                },
                enabled = title.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
