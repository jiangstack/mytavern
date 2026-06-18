package org.jiangstack.mytavern.ui.interactive

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jiangstack.mytavern.MyTavernApplication
import org.jiangstack.mytavern.R
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.InteractiveGame
import org.jiangstack.mytavern.domain.model.WorldBook
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveGameListScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToPlay: (Long) -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: InteractiveGameListViewModel = viewModel(
        factory = InteractiveGameListViewModel.factory(
            container.interactiveGameRepository,
            container.worldBookRepository,
            container.characterRepository
        )
    )

    val games by viewModel.games.collectAsState()
    val worldBooks by viewModel.worldBooks.collectAsState()
    val aiCharacters by viewModel.aiCharacters.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var gameToDelete by remember { mutableStateOf<InteractiveGame?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.interactive_list_title)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.interactive_create_title))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (games.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.interactive_list_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(games.size) { index ->
                        val game = games[index]
                        val playCharacterName = aiCharacters.find { it.id == game.playCharacterId }?.name ?: ""
                        InteractiveGameItem(
                            game = game,
                            playCharacterName = playCharacterName,
                            onClick = { onNavigateToPlay(game.id) },
                            onLongClick = {
                                gameToDelete = game
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        InteractiveGameCreateDialog(
            worldBooks = worldBooks,
            aiCharacters = aiCharacters,
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, narratorStyle, storyBackground, storyMainPlot, windowWordCount, playCharacterId, worldBookId, characterIds ->
                viewModel.createGame(title, narratorStyle, storyBackground, storyMainPlot, windowWordCount, playCharacterId, worldBookId, characterIds)
                showCreateDialog = false
            }
        )
    }

    if (showDeleteDialog && gameToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.interactive_delete_title)) },
            text = {
                Text(stringResource(R.string.interactive_delete_message, gameToDelete!!.title))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGame(gameToDelete!!)
                        showDeleteDialog = false
                        gameToDelete = null
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
private fun InteractiveGameItem(
    game: InteractiveGame,
    playCharacterName: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

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
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = game.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (playCharacterName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.interactive_play_character_label, playCharacterName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (game.storyBackground.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = game.storyBackground,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateFormat.format(Date(game.updatedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun InteractiveGameCreateDialog(
    worldBooks: List<WorldBook>,
    aiCharacters: List<Character>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, narratorStyle: String, storyBackground: String, storyMainPlot: String, windowWordCount: Int, playCharacterId: Long, worldBookId: Long?, characterIds: List<Long>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var narratorStyle by remember { mutableStateOf("") }
    var storyBackground by remember { mutableStateOf("") }
    var storyMainPlot by remember { mutableStateOf("") }
    var windowWordCountText by remember { mutableStateOf("3000") }
    var selectedWorldBookId by remember { mutableStateOf<Long?>(null) }
    var worldBookExpanded by remember { mutableStateOf(false) }
    val selectedCharacterIds = remember { mutableStateListOf<Long>() }
    var selectedPlayCharacterId by remember { mutableStateOf<Long?>(null) }
    var playCharacterExpanded by remember { mutableStateOf(false) }

    val selectedWorldBookName = worldBooks.find { it.id == selectedWorldBookId }?.name
    val selectedPlayCharacterName = aiCharacters.find { it.id == selectedPlayCharacterId }?.name

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.interactive_create_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.interactive_game_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = narratorStyle,
                    onValueChange = { narratorStyle = it },
                    label = { Text(stringResource(R.string.interactive_narrator_style)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = storyBackground,
                    onValueChange = { storyBackground = it },
                    label = { Text(stringResource(R.string.interactive_story_background)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = storyMainPlot,
                    onValueChange = { storyMainPlot = it },
                    label = { Text(stringResource(R.string.interactive_story_main_plot)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = windowWordCountText,
                    onValueChange = { windowWordCountText = it },
                    label = { Text(stringResource(R.string.interactive_window_word_count)) },
                    singleLine = true,
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
                        label = { Text(stringResource(R.string.interactive_select_worldbook)) },
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
                            text = { Text(stringResource(R.string.interactive_worldbook_none)) },
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
                    text = stringResource(R.string.interactive_select_characters),
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
                                    if (selectedPlayCharacterId == character.id) {
                                        selectedPlayCharacterId = null
                                    }
                                } else {
                                    selectedCharacterIds.add(character.id)
                                }
                            },
                            label = { Text(character.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 扮演角色单选（从已选角色中选择）
                val selectableCharacters = aiCharacters.filter { selectedCharacterIds.contains(it.id) }
                ExposedDropdownMenuBox(
                    expanded = playCharacterExpanded,
                    onExpandedChange = { playCharacterExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedPlayCharacterName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.interactive_play_character)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = playCharacterExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = playCharacterExpanded,
                        onDismissRequest = { playCharacterExpanded = false }
                    ) {
                        selectableCharacters.forEach { character ->
                            DropdownMenuItem(
                                text = { Text(character.name) },
                                onClick = {
                                    selectedPlayCharacterId = character.id
                                    playCharacterExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val windowWordCount = windowWordCountText.toIntOrNull() ?: 3000
                    onConfirm(
                        title, narratorStyle, storyBackground, storyMainPlot,
                        windowWordCount, selectedPlayCharacterId ?: 0L,
                        selectedWorldBookId, selectedCharacterIds.toList()
                    )
                },
                enabled = title.isNotBlank() && selectedPlayCharacterId != null && selectedCharacterIds.isNotEmpty()
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
