package org.jiangstack.mytavern.ui.interactive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jiangstack.mytavern.MyTavernApplication
import org.jiangstack.mytavern.R
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.InteractiveGame
import org.jiangstack.mytavern.domain.model.WorldBook

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveGameEditScreen(
    gameId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToPromptSettings: () -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: InteractiveGameEditViewModel = viewModel(
        key = "edit_$gameId",
        factory = InteractiveGameEditViewModel.factory(
            gameId,
            container.interactiveGameRepository,
            container.worldBookRepository,
            container.characterRepository
        )
    )

    val game by viewModel.game.collectAsState()
    val worldBooks by viewModel.worldBooks.collectAsState()
    val aiCharacters by viewModel.aiCharacters.collectAsState()

    val currentGame = game
    if (currentGame == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.interactive_edit_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {}
        }
        return
    }

    InteractiveGameEditContent(
        game = currentGame,
        worldBooks = worldBooks,
        aiCharacters = aiCharacters,
        onNavigateBack = onNavigateBack,
        onNavigateToPromptSettings = onNavigateToPromptSettings,
        onSave = { title, narratorStyle, storyBackground, storyMainPlot, windowWordCount, playCharacterId, worldBookId, characterIds ->
            viewModel.updateGame(title, narratorStyle, storyBackground, storyMainPlot, windowWordCount, playCharacterId, worldBookId, characterIds)
            onNavigateBack()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun InteractiveGameEditContent(
    game: InteractiveGame,
    worldBooks: List<WorldBook>,
    aiCharacters: List<Character>,
    onNavigateBack: () -> Unit,
    onNavigateToPromptSettings: () -> Unit,
    onSave: (title: String, narratorStyle: String, storyBackground: String, storyMainPlot: String, windowWordCount: Int, playCharacterId: Long, worldBookId: Long?, characterIds: List<Long>) -> Unit
) {
    var title by remember(game) { mutableStateOf(game.title) }
    var narratorStyle by remember(game) { mutableStateOf(game.narratorStyle) }
    var storyBackground by remember(game) { mutableStateOf(game.storyBackground) }
    var storyMainPlot by remember(game) { mutableStateOf(game.storyMainPlot) }
    var windowWordCountText by remember(game) { mutableStateOf(game.windowWordCount.toString()) }
    var selectedWorldBookId by remember(game) { mutableStateOf(game.worldBookId) }
    var worldBookExpanded by remember { mutableStateOf(false) }
    val selectedCharacterIds = remember(game) { mutableStateListOf<Long>().apply { addAll(game.characterIds) } }
    var selectedPlayCharacterId by remember(game) { mutableStateOf(game.playCharacterId) }
    var playCharacterExpanded by remember { mutableStateOf(false) }

    val selectedWorldBookName = worldBooks.find { it.id == selectedWorldBookId }?.name
    val selectedPlayCharacterName = aiCharacters.find { it.id == selectedPlayCharacterId }?.name

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.interactive_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToPromptSettings) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = stringResource(R.string.interactive_prompt_settings)
                        )
                    }
                    TextButton(
                        onClick = {
                            val windowWordCount = windowWordCountText.toIntOrNull() ?: 3000
                            onSave(
                                title, narratorStyle, storyBackground, storyMainPlot,
                                windowWordCount, selectedPlayCharacterId,
                                selectedWorldBookId, selectedCharacterIds.toList()
                            )
                        },
                        enabled = title.isNotBlank() && selectedPlayCharacterId != 0L && selectedCharacterIds.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.interactive_game_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = narratorStyle,
                onValueChange = { narratorStyle = it },
                label = { Text(stringResource(R.string.interactive_narrator_style)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(12.dp))

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
                                    selectedPlayCharacterId = 0L
                                }
                            } else {
                                selectedCharacterIds.add(character.id)
                            }
                        },
                        label = { Text(character.name) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // 扮演角色单选
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
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = storyBackground,
                onValueChange = { storyBackground = it },
                label = { Text(stringResource(R.string.interactive_story_background)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = storyMainPlot,
                onValueChange = { storyMainPlot = it },
                label = { Text(stringResource(R.string.interactive_story_main_plot)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = windowWordCountText,
                onValueChange = { windowWordCountText = it },
                label = { Text(stringResource(R.string.interactive_window_word_count)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
