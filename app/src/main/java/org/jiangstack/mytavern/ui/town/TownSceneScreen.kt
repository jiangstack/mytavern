package org.jiangstack.mytavern.ui.town

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import org.jiangstack.mytavern.MyTavernApplication
import org.jiangstack.mytavern.R
import org.jiangstack.mytavern.domain.model.SceneLine
import org.jiangstack.mytavern.domain.model.SceneStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TownSceneScreen(
    townId: Long,
    sceneId: Long,
    onNavigateBack: () -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: TownSceneViewModel = viewModel(
        key = "town_scene_$sceneId",
        factory = TownSceneViewModel.factory(
            townId,
            sceneId,
            container.townRepository,
            container.characterRepository,
            container.townSimulationService
        )
    )

    val scene by viewModel.scene.collectAsState()
    val town by viewModel.town.collectAsState()
    val locationName by viewModel.locationName.collectAsState()
    val participants by viewModel.participants.collectAsState()
    val lines by viewModel.lines.collectAsState()
    val streamingText by viewModel.streamingText.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val endBusy by viewModel.endBusy.collectAsState()
    val error by viewModel.error.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isDone by viewModel.isDone.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val errorTemplate = stringResource(R.string.town_error, "")
    var showEndConfirm by remember { mutableStateOf(false) }
    var showForceStopConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(errorTemplate + it)
            viewModel.clearError()
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(lines.size, streamingText) {
        val count = lines.size + if (streamingText != null) 1 else 0
        if (count > 0) {
            listState.animateScrollToItem(count - 1)
        }
    }

    val isInteractive = scene?.status == SceneStatus.INTERACTIVE || scene?.status == SceneStatus.AWAITING_PLAYER

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = locationName.ifBlank { stringResource(R.string.town_scene_title) },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        scene?.let { s ->
                            Text(
                                text = stringResource(R.string.town_day_hour, s.day, s.hour),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(android.R.string.cancel))
                    }
                },
                actions = {
                    when {
                        // 生成中有台词：显示强制结束
                        (scene?.status == SceneStatus.GENERATING || scene?.status == SceneStatus.PENDING) && isStreaming && lines.isNotEmpty() -> {
                            TextButton(
                                onClick = { showForceStopConfirm = true }
                            ) { Text(stringResource(R.string.town_scene_force_stop)) }
                        }
                        // 互动进行中且未在流式：显示结束场景
                        isInteractive && !isStreaming -> {
                            TextButton(
                                enabled = !endBusy,
                                onClick = { showEndConfirm = true }
                            ) { Text(stringResource(R.string.town_scene_end)) }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            when {
                isInteractive && !isStreaming -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { viewModel.updateInput(it) },
                            label = { Text(stringResource(R.string.town_scene_input_hint)) },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.sendInput() }, enabled = inputText.isNotBlank()) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.town_scene_input_hint))
                        }
                    }
                }
                isStreaming -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 参与者头像行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                participants.forEach { p ->
                    AsyncImage(
                        model = p.avatarUri,
                        contentDescription = p.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = participants.joinToString("、") { it.name },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(lines) { line ->
                    SceneLineItem(
                        line = line,
                        avatarUri = participants.firstOrNull { it.memberId == line.speakerId }?.avatarUri
                    )
                }
                if (streamingText != null) {
                    item {
                        Text(
                            text = streamingText!! + " ▌",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        )
                    }
                }
                if (isStreaming && lines.isEmpty() && streamingText == null) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.town_scene_generating),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (isDone && scene?.summary?.isNotBlank() == true) {
                    item {
                        Card(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    stringResource(R.string.town_scene_summary),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    scene?.summary ?: "",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEndConfirm) {
        AlertDialog(
            onDismissRequest = { if (!endBusy) showEndConfirm = false },
            title = { Text(stringResource(R.string.town_scene_end)) },
            text = { Text(stringResource(R.string.town_scene_end_confirm)) },
            confirmButton = {
                TextButton(
                    enabled = !endBusy,
                    onClick = {
                        showEndConfirm = false
                        viewModel.endScene { /* 状态刷新，摘要展示 */ }
                    }
                ) { Text(stringResource(R.string.town_scene_end)) }
            },
            dismissButton = {
                TextButton(
                    enabled = !endBusy,
                    onClick = { showEndConfirm = false }
                ) { Text(stringResource(android.R.string.cancel)) }
            }
        )
    }

    if (showForceStopConfirm) {
        AlertDialog(
            onDismissRequest = { showForceStopConfirm = false },
            title = { Text(stringResource(R.string.town_scene_force_stop)) },
            text = { Text(stringResource(R.string.town_scene_force_stop_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showForceStopConfirm = false
                        viewModel.forceStopScene()
                    }
                ) { Text(stringResource(R.string.town_scene_force_stop)) }
            },
            dismissButton = {
                TextButton(onClick = { showForceStopConfirm = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SceneLineItem(line: SceneLine, avatarUri: String?) {
    when (line.kind) {
        "narration" -> {
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
        "dialogue" -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = line.speakerName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = line.speakerName ?: "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
        "action" -> {
            Text(
                text = buildString {
                    line.speakerName?.let { append("$it ") }
                    append(line.text)
                },
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp, top = 4.dp, bottom = 4.dp)
            )
        }
        else -> {
            Text(
                text = "（${line.text}）",
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp, top = 4.dp, bottom = 4.dp)
            )
        }
    }
}
