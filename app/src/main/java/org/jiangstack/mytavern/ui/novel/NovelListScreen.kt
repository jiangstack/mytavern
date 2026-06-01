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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jiangstack.mytavern.MyTavernApplication
import org.jiangstack.mytavern.R
import org.jiangstack.mytavern.domain.model.Novel
import org.jiangstack.mytavern.domain.model.WorldBook
import org.jiangstack.mytavern.domain.model.Character

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelListScreen(
    onNavigateToDetail: (Long) -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: NovelListViewModel = viewModel(
        factory = NovelListViewModel.factory(
            container.novelRepository,
            container.worldBookRepository,
            container.characterRepository
        )
    )

    val novels by viewModel.novels.collectAsState()
    val worldBooks by viewModel.worldBooks.collectAsState()
    val aiCharacters by viewModel.aiCharacters.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var novelToDelete by remember { mutableStateOf<Novel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.novel_list_title)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.novel_create_title))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (novels.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.novel_list_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(novels, key = { it.id }) { novel ->
                        NovelItem(
                            novel = novel,
                            worldBooks = worldBooks,
                            onClick = { onNavigateToDetail(novel.id) },
                            onLongClick = {
                                novelToDelete = novel
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        NovelCreateDialog(
            worldBooks = worldBooks,
            aiCharacters = aiCharacters,
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, description, worldBookId, characterIds ->
                viewModel.createNovel(title, description, worldBookId, characterIds)
                showCreateDialog = false
            }
        )
    }

    if (showDeleteDialog && novelToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.novel_delete_title)) },
            text = {
                Text(stringResource(R.string.novel_delete_message, novelToDelete!!.title))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNovel(novelToDelete!!)
                        showDeleteDialog = false
                        novelToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NovelItem(
    novel: Novel,
    worldBooks: List<WorldBook>,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val worldBookName = worldBooks.find { it.id == novel.worldBookId }?.name

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = novel.title,
                style = MaterialTheme.typography.titleMedium
            )
            if (novel.description.isNotBlank()) {
                Text(
                    text = novel.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (worldBookName != null) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.height(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = worldBookName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                if (novel.characterIds.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.height(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${novel.characterIds.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun NovelCreateDialog(
    worldBooks: List<WorldBook>,
    aiCharacters: List<Character>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, worldBookId: Long?, characterIds: List<Long>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedWorldBookId by remember { mutableStateOf<Long?>(null) }
    var worldBookExpanded by remember { mutableStateOf(false) }
    val selectedCharacterIds = remember { mutableStateListOf<Long>() }

    val selectedWorldBookName = worldBooks.find { it.id == selectedWorldBookId }?.name

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.novel_create_title)) },
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

                // 世界书下拉选择
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

                // 角色多选
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
