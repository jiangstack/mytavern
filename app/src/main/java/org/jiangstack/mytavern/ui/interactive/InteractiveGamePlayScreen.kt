package org.jiangstack.mytavern.ui.interactive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jiangstack.mytavern.MyTavernApplication
import org.jiangstack.mytavern.R
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.InteractiveCheckpoint
import org.jiangstack.mytavern.domain.model.InteractiveGameState
import org.jiangstack.mytavern.domain.model.InteractiveMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveGamePlayScreen(
    gameId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: InteractiveGamePlayViewModel = viewModel(
        key = "play_$gameId",
        factory = InteractiveGamePlayViewModel.factory(
            gameId,
            container.interactiveGameRepository,
            container.characterRepository,
            container.interactiveStoryService,
            container.userPreferencesRepository
        )
    )

    val game by viewModel.game.collectAsState()
    val characters by viewModel.characters.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val gameState by viewModel.gameState.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val currentStoryText by viewModel.currentStoryText.collectAsState()
    val actionOptions by viewModel.actionOptions.collectAsState()
    val error by viewModel.error.collectAsState()
    val storyWordCount by viewModel.storyWordCount.collectAsState()
    val contextBoundaryIndex by viewModel.contextBoundaryIndex.collectAsState()
    val dialogueHighlightEnabled by viewModel.dialogueHighlightEnabled.collectAsState()
    val dialogueHighlightColor by viewModel.dialogueHighlightColor.collectAsState()
    val checkpoints by viewModel.checkpoints.collectAsState()
    val checkpointLoaded by viewModel.checkpointLoaded.collectAsState()


    var customAction by remember { mutableStateOf("") }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showCheckpointSheet by remember { mutableStateOf(false) }
    var checkpointToRename by remember { mutableStateOf<InteractiveCheckpoint?>(null) }
    var checkpointToDelete by remember { mutableStateOf<InteractiveCheckpoint?>(null) }
    var selectedCharacterId by remember { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    val characterNameColor = MaterialTheme.colorScheme.primary
    val characterNames = remember(characters) { characters.map { it.name to it.id } }

    // Auto-scroll to bottom when new content arrives
    LaunchedEffect(messages.size, currentStoryText) {
        if (messages.isNotEmpty() || currentStoryText.isNotBlank()) {
            listState.animateScrollToItem(
                if (currentStoryText.isNotBlank()) messages.size + 1 else messages.size
            )
        }
    }

    // Show error as snackbar
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Show checkpoint loaded snackbar
    LaunchedEffect(checkpointLoaded) {
        checkpointLoaded?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearCheckpointLoaded()
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(game?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(gameId) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.interactive_edit_title))
                    }
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.interactive_settings))
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Story content area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val displayedMessages = messages.filter { it.role != "user" }
                itemsIndexed(displayedMessages) { index, msg ->
                    StoryParagraph(
                        message = msg,
                        isContextBoundary = index == contextBoundaryIndex,
                        dialogueEnabled = dialogueHighlightEnabled,
                        dialogueColor = if (dialogueHighlightEnabled) Color(dialogueHighlightColor) else Color.Unspecified,
                        characterNameColor = characterNameColor,
                        characterNames = characterNames,
                        onCharacterClick = { selectedCharacterId = it }
                    )
                }

                // Streaming content
                if (currentStoryText.isNotBlank()) {
                    item {
                        StoryParagraph(
                            message = InteractiveMessage(
                                gameId = gameId,
                                role = "narrator",
                                content = currentStoryText
                            ),
                            dialogueEnabled = dialogueHighlightEnabled,
                            dialogueColor = if (dialogueHighlightEnabled) Color(dialogueHighlightColor) else Color.Unspecified,
                            characterNameColor = characterNameColor,
                            characterNames = characterNames,
                            onCharacterClick = { selectedCharacterId = it }
                        )
                    }
                }

                // Loading indicator
                if (isStreaming && currentStoryText.isBlank()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }

            // Action options and custom input area
            if (!isStreaming) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    CustomActionInput(
                        value = customAction,
                        onValueChange = { customAction = it },
                        onSend = {
                            if (customAction.isNotBlank()) {
                                viewModel.startNewTurn(customAction)
                                customAction = ""
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (actionOptions.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            actionOptions.forEach { option ->
                                OutlinedButton(
                                    onClick = { viewModel.startNewTurn(option) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(option)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.interactive_story_word_count, storyWordCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(onClick = { showCheckpointSheet = true }) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.interactive_checkpoints))
                            }
                            FilledTonalButton(onClick = { viewModel.continueStory() }) {
                                Icon(Icons.Default.SkipNext, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.interactive_continue))
                            }
                        }
                    }
                }
            } else {
                // Stop button during streaming
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = { viewModel.stopStreaming() }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.interactive_stop))
                    }
                }
            }
        }
    }

    // Character info dialog
    val selectedCharacter = selectedCharacterId?.let { id -> characters.find { it.id == id } }
    selectedCharacter?.let { character ->
        CharacterInfoDialog(
            character = character,
            gameState = gameState,
            playCharacterId = game?.playCharacterId,
            onDismiss = { selectedCharacterId = null }
        )
    }

    // Settings bottom sheet
    if (showSettingsSheet) {
        SettingsBottomSheet(
            gameState = gameState,
            onDismiss = { showSettingsSheet = false },
            onSave = { env, status, items ->
                viewModel.updateGameState(env, status, items)
                showSettingsSheet = false
            },
            onClearStory = { viewModel.clearStory() }
        )
    }

    // Checkpoint bottom sheet
    if (showCheckpointSheet) {
        CheckpointBottomSheet(
            checkpoints = checkpoints,
            onDismiss = { showCheckpointSheet = false },
            onLoad = {
                viewModel.loadCheckpoint(it.id)
                showCheckpointSheet = false
            },
            onRename = { checkpointToRename = it },
            onDelete = { checkpointToDelete = it }
        )
    }

    checkpointToRename?.let { checkpoint ->
        RenameCheckpointDialog(
            initialName = checkpoint.name,
            onDismiss = { checkpointToRename = null },
            onConfirm = { name ->
                viewModel.renameCheckpoint(checkpoint.id, name)
                checkpointToRename = null
            }
        )
    }

    checkpointToDelete?.let { checkpoint ->
        DeleteCheckpointDialog(
            checkpoint = checkpoint,
            onDismiss = { checkpointToDelete = null },
            onConfirm = {
                viewModel.deleteCheckpoint(checkpoint)
                checkpointToDelete = null
            }
        )
    }
}

@Composable
private fun StoryParagraph(
    message: InteractiveMessage,
    isContextBoundary: Boolean = false,
    dialogueEnabled: Boolean,
    dialogueColor: Color,
    characterNameColor: Color,
    characterNames: List<Pair<String, Long>>,
    onCharacterClick: (Long) -> Unit
) {
    val annotatedString = remember(
        message.content,
        dialogueEnabled,
        dialogueColor,
        characterNames,
        characterNameColor
    ) {
        if (dialogueEnabled) {
            buildInteractiveStoryAnnotatedString(
                text = message.content,
                dialogueColor = dialogueColor,
                characterNames = characterNames,
                characterNameColor = characterNameColor
            )
        } else {
            buildInteractiveStoryAnnotatedString(
                text = message.content,
                dialogueColor = Color.Unspecified,
                characterNames = characterNames,
                characterNameColor = characterNameColor
            )
    }
    }
    val errorColor = MaterialTheme.colorScheme.error
    val displayAnnotatedString = if (isContextBoundary) {
        remember(annotatedString, isContextBoundary, errorColor) {
            AnnotatedString.Builder().apply {
                pushStyle(SpanStyle(color = errorColor))
                append("|")
                pop()
                append(annotatedString)
            }.toAnnotatedString()
        }
    } else annotatedString
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = displayAnnotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(
            textIndent = TextIndent(firstLine = 32.sp)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(displayAnnotatedString) {
                detectTapGestures { offset ->
                    textLayoutResult?.let { layout ->
                        val position = layout.getOffsetForPosition(offset)
                        displayAnnotatedString
                            .getStringAnnotations("characterId", position, position)
                            .firstOrNull()
                            ?.let { annotation ->
                                onCharacterClick(annotation.item.toLong())
                            }
                    }
                }
            },
        onTextLayout = { textLayoutResult = it }
    )
    }

fun buildInteractiveStoryAnnotatedString(
    text: String,
    dialogueColor: Color,
    characterNames: List<Pair<String, Long>>,
    characterNameColor: Color
): AnnotatedString {

    val dialogueRegex = Regex("([\u300c\u300e\u201c\u2018])(.*?)([\u300d\u300f\u201d\u2019])")
    val dialogueSpan = SpanStyle(color = dialogueColor)
    val nameSpan = SpanStyle(color = characterNameColor)

    data class Interval(
        val start: Int,
        val end: Int,
        val style: SpanStyle,
        val annotationValue: String? = null
    )

    // Match character names (longest first) with simple boundary checks to avoid
    // matching short names inside longer words (e.g. "明" inside "明天").
    val nameIntervals = mutableListOf<Interval>()
    val used = BooleanArray(text.length)
    val sortedNames = characterNames
        .filter { it.first.isNotBlank() }
        .distinctBy { it.first }
        .sortedByDescending { it.first.length }
    for ((name, id) in sortedNames) {
        var start = 0
        while (start <= text.length - name.length) {
            val idx = text.indexOf(name, start)
            if (idx < 0) break
            val end = idx + name.length
            val prev = text.getOrNull(idx - 1)
            val next = text.getOrNull(end)
            val boundaryOk = (prev == null || !prev.isLetterOrDigit()) &&
                (next == null || !next.isLetterOrDigit())
            val overlap = (idx until end).any { used[it] }
            if (boundaryOk && !overlap) {
                nameIntervals.add(Interval(idx, end, nameSpan, id.toString()))
                for (i in idx until end) used[i] = true
                start = end
            } else {
                start = idx + 1
            }
        }
    }

    // Match dialogue ranges (including quotation marks).
    val dialogueIntervals = dialogueRegex
        .findAll(text)
        .map { Interval(it.range.first, it.range.last + 1, dialogueSpan) }
        .toList()

    val allIntervals = nameIntervals + dialogueIntervals
    if (allIntervals.isEmpty()) {
        return AnnotatedString(text)
    }

    val boundaries = sortedSetOf(0, text.length)
    allIntervals.forEach {
        boundaries.add(it.start)
        boundaries.add(it.end)
    }
    val points = boundaries.toList()

    val builder = AnnotatedString.Builder()
    val active = mutableListOf<Interval>()
    val startPositions = mutableMapOf<Interval, Int>()

    for (i in 0 until points.size - 1) {
        val pos = points[i]
        val nextPos = points[i + 1]

        // Pop intervals that end at this position (innermost first).
        val iterator = active.listIterator(active.size)
        while (iterator.hasPrevious()) {
            val interval = iterator.previous()
            if (interval.end == pos) {
                interval.annotationValue?.let { value ->
                    val startPos = startPositions[interval] ?: 0
                    builder.addStringAnnotation("characterId", value, startPos, builder.length)
                }
                builder.pop()
                iterator.remove()
            }
        }

        // Push intervals that start at this position, outermost first.
        val starting = allIntervals.filter { it.start == pos }.sortedByDescending { it.end }
        for (interval in starting) {
            active.add(interval)
            builder.pushStyle(interval.style)
            if (interval.annotationValue != null) {
                startPositions[interval] = builder.length
            }
        }

        builder.append(text.substring(pos, nextPos))
    }

    // Pop any remaining active intervals at the end of the text.
    val iterator = active.listIterator(active.size)
    while (iterator.hasPrevious()) {
        val interval = iterator.previous()
        interval.annotationValue?.let { value ->
            val startPos = startPositions[interval] ?: 0
            builder.addStringAnnotation("characterId", value, startPos, builder.length)
        }
        builder.pop()
        iterator.remove()
    }

    return builder.toAnnotatedString()
}

@Composable
private fun CustomActionInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(stringResource(R.string.interactive_custom_action_hint)) },
            modifier = Modifier.weight(1f),
            singleLine = false,
            maxLines = 3
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onSend,
            enabled = value.isNotBlank()
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.interactive_send))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterInfoDialog(
    character: Character,
    gameState: InteractiveGameState?,
    playCharacterId: Long?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(character.name)
                if (character.id == playCharacterId) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.interactive_play_character_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column {
                if (character.description.isNotBlank()) {
                    Text(
                        text = character.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                gameState?.let { state ->
                    if (state.environment.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.interactive_environment) + "：" + state.environment,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (state.characterStatus.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.interactive_character_status) + "：" + state.characterStatus,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (state.characterItems.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.interactive_character_items) + "：" + state.characterItems,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsBottomSheet(
    gameState: InteractiveGameState?,
    onDismiss: () -> Unit,
    onSave: (environment: String, characterStatus: String, characterItems: String) -> Unit,
    onClearStory: () -> Unit
) {
    var environment by remember(gameState) { mutableStateOf(gameState?.environment ?: "") }
    var characterStatus by remember(gameState) { mutableStateOf(gameState?.characterStatus ?: "") }
    var characterItems by remember(gameState) { mutableStateOf(gameState?.characterItems ?: "") }
    var showClearConfirm by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.interactive_clear_story_title)) },
            text = { Text(stringResource(R.string.interactive_clear_story_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearStory()
                        showClearConfirm = false
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.interactive_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.interactive_settings),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = environment,
                onValueChange = { environment = it },
                label = { Text(stringResource(R.string.interactive_environment)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = characterStatus,
                onValueChange = { characterStatus = it },
                label = { Text(stringResource(R.string.interactive_character_status)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = characterItems,
                onValueChange = { characterItems = it },
                label = { Text(stringResource(R.string.interactive_character_items)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { showClearConfirm = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.interactive_clear_story))
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { onSave(environment, characterStatus, characterItems) }) {
                    Text(stringResource(R.string.save))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckpointBottomSheet(
    checkpoints: List<InteractiveCheckpoint>,
    onDismiss: () -> Unit,
    onLoad: (InteractiveCheckpoint) -> Unit,
    onRename: (InteractiveCheckpoint) -> Unit,
    onDelete: (InteractiveCheckpoint) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val treeNodes = remember(checkpoints) { buildCheckpointTree(checkpoints) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.interactive_checkpoint_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (treeNodes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.interactive_checkpoint_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(treeNodes, key = { _, node -> node.checkpoint.id }) { _, node ->
                        CheckpointTreeItem(
                            node = node,
                            onLoad = onLoad,
                            onRename = onRename,
                            onDelete = onDelete
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CheckpointTreeItem(
    node: CheckpointTreeNode,
    onLoad: (InteractiveCheckpoint) -> Unit,
    onRename: (InteractiveCheckpoint) -> Unit,
    onDelete: (InteractiveCheckpoint) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (node.depth * 24).dp)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = node.checkpoint.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .clickable { onLoad(node.checkpoint) }
                .padding(vertical = 4.dp)
        )
        Row {
            IconButton(onClick = { onRename(node.checkpoint) }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.interactive_checkpoint_rename)
                )
            }
            IconButton(onClick = { onDelete(node.checkpoint) }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.interactive_checkpoint_delete)
                )
            }
        }
    }
}

private data class CheckpointTreeNode(
    val checkpoint: InteractiveCheckpoint,
    val depth: Int
)

private fun buildCheckpointTree(checkpoints: List<InteractiveCheckpoint>): List<CheckpointTreeNode> {
    val byParent = checkpoints.groupBy { it.parentId }
    val result = mutableListOf<CheckpointTreeNode>()
    val roots = checkpoints.filter { it.parentId == null }.sortedBy { it.createdAt }

    fun traverse(checkpoint: InteractiveCheckpoint, depth: Int) {
        result.add(CheckpointTreeNode(checkpoint, depth))
        byParent[checkpoint.id]
            ?.sortedBy { it.createdAt }
            ?.forEach { traverse(it, depth + 1) }
    }

    roots.forEach { traverse(it, 0) }
    return result
}

@Composable
private fun RenameCheckpointDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.interactive_checkpoint_rename)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.interactive_checkpoint_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.trim().isNotEmpty()
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
private fun DeleteCheckpointDialog(
    checkpoint: InteractiveCheckpoint,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.interactive_checkpoint_delete)) },
        text = {
            Text(
                stringResource(
                    R.string.interactive_checkpoint_delete_message,
                    checkpoint.name
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
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
