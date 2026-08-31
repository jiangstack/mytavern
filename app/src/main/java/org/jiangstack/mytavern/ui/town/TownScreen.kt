package org.jiangstack.mytavern.ui.town

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import org.jiangstack.mytavern.domain.model.LogKind
import org.jiangstack.mytavern.domain.model.SceneStatus
import org.jiangstack.mytavern.domain.model.SceneType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TownScreen(
    townId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    onNavigateToScene: (Long) -> Unit,
    onNavigateToPromptSettings: () -> Unit = {}
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: TownViewModel = viewModel(
        key = "town_$townId",
        factory = TownViewModel.factory(
            townId,
            container.townRepository,
            container.characterRepository,
            container.townSimulationService
        )
    )

    val town by viewModel.town.collectAsState()
    val memberDisplays by viewModel.memberDisplays.collectAsState()
    val locations by viewModel.locations.collectAsState()
    val relationshipDisplays by viewModel.relationshipDisplays.collectAsState()
    val scenes by viewModel.scenes.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val snapshots by viewModel.snapshots.collectAsState()
    val advancing by viewModel.advancing.collectAsState()
    val progressHours by viewModel.progressHours.collectAsState()
    val injectingEvent by viewModel.injectingEvent.collectAsState()
    val snapshotBusy by viewModel.snapshotBusy.collectAsState()
    val error by viewModel.error.collectAsState()
    val pendingSceneId by viewModel.pendingSceneId.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showEventDialog by remember { mutableStateOf(false) }
    var showSnapshotSheet by remember { mutableStateOf(false) }
    var snapshotToRestore by remember { mutableStateOf<TownSnapshotRestore?>(null) }
    var selectedMemberId by remember { mutableStateOf<Long?>(null) }

    val errorTemplate = stringResource(R.string.town_error, "")

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(errorTemplate + it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(pendingSceneId) {
        pendingSceneId?.let { sceneId ->
            viewModel.consumePendingScene()
            onNavigateToScene(sceneId)
        }
    }

    val activeScene = scenes.firstOrNull {
        it.status == SceneStatus.INTERACTIVE || it.status == SceneStatus.AWAITING_PLAYER
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        town?.name ?: stringResource(R.string.nav_town),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(android.R.string.cancel))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.town_edit_title))
                    }
                    IconButton(onClick = onNavigateToPromptSettings) {
                        Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.town_prompt_settings_title))
                    }
                    IconButton(onClick = { showSnapshotSheet = true }) {
                        Icon(Icons.Default.Save, contentDescription = stringResource(R.string.town_snapshot))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showEventDialog = true },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                text = { Text(stringResource(R.string.town_world_event)) }
            )
        }
    ) { innerPadding ->
        if (memberDisplays.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.town_members_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onNavigateToEdit) {
                    Text(stringResource(R.string.town_edit_title))
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 时间与推进
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = town?.let { stringResource(R.string.town_day_hour, it.currentDay, it.currentHour) } ?: "",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                enabled = !advancing && activeScene == null,
                                onClick = { viewModel.advanceToNextEvent() },
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.town_advance_next)) }
                            OutlinedButton(
                                enabled = !advancing && activeScene == null,
                                onClick = { viewModel.advanceOneHour() },
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.town_advance_hour)) }
                        }
                        if (advancing) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (progressHours != null) {
                                    stringResource(R.string.town_advancing, progressHours!!)
                                } else {
                                    stringResource(R.string.town_scene_generating)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (activeScene != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.town_scene_ongoing),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.clickable { onNavigateToScene(activeScene.id) }
                            )
                        }
                    }
                }
            }

            // 居民动态
            item {
                Text(
                    stringResource(R.string.town_members_status),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(memberDisplays, key = { "member_${it.member.id}" }) { display ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMemberId = display.member.id }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = display.avatarUri,
                            contentDescription = display.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(display.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = buildString {
                                    append(display.locationName.ifBlank { stringResource(R.string.town_unknown_location) })
                                    val activity = display.member.currentActivity.ifBlank { stringResource(R.string.town_activity_free) }
                                    append(" · $activity")
                                    if (display.member.mood.isNotBlank()) append(" · ${display.member.mood}")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (display.member.isPlayerControlled) {
                            var moveMenu by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { moveMenu = true }) {
                                    Text(stringResource(R.string.town_move_to))
                                }
                                DropdownMenu(
                                    expanded = moveMenu,
                                    onDismissRequest = { moveMenu = false }
                                ) {
                                    locations.forEach { location ->
                                        DropdownMenuItem(
                                            text = { Text(location.name) },
                                            onClick = {
                                                moveMenu = false
                                                viewModel.movePlayerTo(location.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 关系
            item {
                Text(
                    stringResource(R.string.town_relationships),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (relationshipDisplays.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.town_relationships_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(relationshipDisplays.size) { index ->
                    val rel = relationshipDisplays[index]
                    Text(
                        text = "${rel.nameA} ↔ ${rel.nameB} · ${stringResource(R.string.town_relationship_affinity, rel.affinity)}" +
                            if (rel.note.isNotBlank()) " · ${rel.note}" else "",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // 场景
            item {
                Text(
                    stringResource(R.string.town_scenes),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (scenes.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.town_scenes_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(scenes.take(6), key = { "scene_${it.id}" }) { scene ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToScene(scene.id) },
                        colors = CardDefaults.cardColors()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.town_day_hour, scene.day, scene.hour),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val statusText = when (scene.status) {
                                    SceneStatus.PENDING -> stringResource(R.string.town_scene_pending)
                                    SceneStatus.GENERATING -> stringResource(R.string.town_scene_generating)
                                    SceneStatus.INTERACTIVE, SceneStatus.AWAITING_PLAYER ->
                                        stringResource(R.string.town_scene_ongoing)
                                    SceneStatus.DONE -> if (scene.type == SceneType.EVENT) stringResource(R.string.town_world_event) else ""
                                }
                                if (statusText.isNotEmpty()) {
                                    Text(statusText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = scene.summary.ifBlank { stringResource(R.string.town_scene_pending) },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 小镇日志
            item {
                Text(
                    stringResource(R.string.town_chronicle),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (logs.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.town_chronicle_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(logs, key = { "log_${it.id}" }) { log ->
                    Text(
                        text = stringResource(R.string.town_day_hour, log.day, log.hour) + "  " + log.text,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = if (log.kind == LogKind.EVENT) FontStyle.Italic else FontStyle.Normal,
                            color = when (log.kind) {
                                LogKind.EVENT -> MaterialTheme.colorScheme.tertiary
                                LogKind.SYSTEM -> MaterialTheme.colorScheme.error
                                LogKind.TICK -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    )
                }
            }
        }
    }

    // 世界事件对话框
    if (showEventDialog) {
        var brief by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!injectingEvent) showEventDialog = false },
            title = { Text(stringResource(R.string.town_world_event)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = brief,
                        onValueChange = { brief = it },
                        label = { Text(stringResource(R.string.town_world_event_hint)) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (injectingEvent) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.town_world_event_generating),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !injectingEvent && brief.isNotBlank(),
                    onClick = {
                        showEventDialog = false
                        viewModel.injectWorldEvent(brief)
                    }
                ) { Text(stringResource(R.string.town_world_event_confirm)) }
            },
            dismissButton = {
                TextButton(
                    enabled = !injectingEvent,
                    onClick = { showEventDialog = false }
                ) { Text(stringResource(android.R.string.cancel)) }
            }
        )
    }

    // 存档 BottomSheet
    if (showSnapshotSheet) {
        var snapshotName by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showSnapshotSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.town_snapshot), style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = snapshotName,
                        onValueChange = { snapshotName = it },
                        label = { Text(stringResource(R.string.town_snapshot_name)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        enabled = !snapshotBusy,
                        onClick = {
                            viewModel.saveSnapshot(snapshotName) { showSnapshotSheet = false }
                        }
                    ) { Text(stringResource(R.string.town_snapshot_save)) }
                }
                if (snapshots.isEmpty()) {
                    Text(
                        stringResource(R.string.town_snapshot_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(snapshots, key = { "snapshot_${it.id}" }) { snapshot ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(snapshot.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        stringResource(R.string.town_day_hour, snapshot.day, snapshot.hour),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(
                                    enabled = !snapshotBusy,
                                    onClick = { snapshotToRestore = TownSnapshotRestore(snapshot.id, snapshot.name) }
                                ) { Text(stringResource(R.string.town_snapshot_restore)) }
                                TextButton(
                                    enabled = !snapshotBusy,
                                    onClick = { viewModel.deleteSnapshot(snapshot.id) }
                                ) {
                                    Text(
                                        stringResource(R.string.town_snapshot_delete),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // 恢复确认
    snapshotToRestore?.let { target ->
        AlertDialog(
            onDismissRequest = { snapshotToRestore = null },
            title = { Text(stringResource(R.string.town_snapshot_restore)) },
            text = { Text(stringResource(R.string.town_snapshot_restore_confirm, target.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        snapshotToRestore = null
                        showSnapshotSheet = false
                        viewModel.restoreSnapshot(target.id) { /* flows 自动刷新 */ }
                    }
                ) { Text(stringResource(R.string.town_snapshot_restore)) }
            },
            dismissButton = {
                TextButton(onClick = { snapshotToRestore = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // 居民详情
    val detailMember = memberDisplays.firstOrNull { it.member.id == selectedMemberId }
    if (detailMember != null) {
        val locNameById = locations.associate { it.id to it.name }
        val memberRelations = relationshipDisplays.filter {
            it.memberAId == detailMember.member.id || it.memberBId == detailMember.member.id
        }
        ModalBottomSheet(onDismissRequest = { selectedMemberId = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = detailMember.avatarUri,
                        contentDescription = detailMember.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(detailMember.name, style = MaterialTheme.typography.titleMedium)
                            if (detailMember.member.isPlayerControlled) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.town_player_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = buildString {
                                append(stringResource(R.string.town_location_label) + "：" + detailMember.locationName.ifBlank { stringResource(R.string.town_unknown_location) })
                                val activity = detailMember.member.currentActivity.ifBlank { stringResource(R.string.town_activity_free) }
                                append("；" + stringResource(R.string.town_doing_label) + "：$activity")
                                if (detailMember.member.mood.isNotBlank()) {
                                    append("；" + stringResource(R.string.town_mood_label) + "：${detailMember.member.mood}")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DetailSection(stringResource(R.string.town_persona)) {
                    Text(
                        detailMember.member.persona.ifBlank { stringResource(R.string.town_detail_empty) },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                DetailSection(stringResource(R.string.town_today_schedule)) {
                    val schedule = detailMember.member.todaySchedule.sortedBy { it.startHour }
                    if (schedule.isEmpty()) {
                        Text(
                            stringResource(R.string.town_detail_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        schedule.forEach { item ->
                            Text(
                                text = stringResource(R.string.town_schedule_time, item.startHour, item.endHour) +
                                    "  " + (locNameById[item.locationId] ?: stringResource(R.string.town_unknown_location)) +
                                    " · " + item.activity,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                DetailSection(stringResource(R.string.town_important_memory)) {
                    val memories = detailMember.member.importantMemory.takeLast(5).reversed()
                    if (memories.isEmpty()) {
                        Text(
                            stringResource(R.string.town_detail_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        memories.forEach { memory ->
                            Text("· ${memory.content}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                DetailSection(stringResource(R.string.town_recent_memory)) {
                    val memories = detailMember.member.recentMemory.takeLast(5).reversed()
                    if (memories.isEmpty()) {
                        Text(
                            stringResource(R.string.town_detail_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        memories.forEach { memory ->
                            Text("· ${memory.content}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                DetailSection(stringResource(R.string.town_relationships)) {
                    if (memberRelations.isEmpty()) {
                        Text(
                            stringResource(R.string.town_relationships_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        memberRelations.forEach { rel ->
                            val otherName = if (rel.memberAId == detailMember.member.id) rel.nameB else rel.nameA
                            Text(
                                text = "· $otherName · " + stringResource(R.string.town_relationship_affinity, rel.affinity) +
                                    if (rel.note.isNotBlank()) " · ${rel.note}" else "",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 详情弹层中的小节：标题 + 内容 */
@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}

private data class TownSnapshotRestore(val id: Long, val name: String)
