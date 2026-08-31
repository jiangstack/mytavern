package org.jiangstack.mytavern.ui.town

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jiangstack.mytavern.MyTavernApplication
import org.jiangstack.mytavern.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TownEditScreen(
    townId: Long,
    onNavigateBack: () -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: TownEditViewModel = viewModel(
        key = "town_edit_$townId",
        factory = TownEditViewModel.factory(
            townId,
            container.townRepository,
            container.characterRepository,
            container.townSimulationService
        )
    )

    val aiCharacters by viewModel.aiCharacters.collectAsState()
    val name by viewModel.name.collectAsState()
    val worldDescription by viewModel.worldDescription.collectAsState()
    val selectedCharacterIds by viewModel.selectedCharacterIds.collectAsState()
    val personaByCharacterId by viewModel.personaByCharacterId.collectAsState()
    val playCharacterId by viewModel.playCharacterId.collectAsState()
    val locationDrafts by viewModel.locationDrafts.collectAsState()
    val saving by viewModel.saving.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val invalidMessage = stringResource(R.string.town_save_invalid)
    val errorPrefix = stringResource(R.string.town_error, "")
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(localError) {
        localError?.let {
            snackbarHostState.showSnackbar(it)
            localError = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.town_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(android.R.string.cancel))
                    }
                },
                actions = {
                    TextButton(
                        enabled = !saving,
                        onClick = {
                            if (name.isBlank() || selectedCharacterIds.isEmpty() || locationDrafts.none { it.name.isNotBlank() }) {
                                localError = invalidMessage
                            } else {
                                viewModel.save(
                                    onSuccess = onNavigateBack,
                                    onError = { localError = errorPrefix + it }
                                )
                            }
                        }
                    ) { Text(stringResource(R.string.town_save_and_generate)) }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (saving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(stringResource(R.string.town_generating_schedule), style = MaterialTheme.typography.bodySmall)
            }

            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.name.value = it },
                label = { Text(stringResource(R.string.town_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = worldDescription,
                onValueChange = { viewModel.worldDescription.value = it },
                label = { Text(stringResource(R.string.town_world_description)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Text(stringResource(R.string.town_members), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.town_members_hint), style = MaterialTheme.typography.bodySmall)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                aiCharacters.forEach { character ->
                    FilterChip(
                        selected = character.id in selectedCharacterIds,
                        onClick = { viewModel.toggleCharacter(character) },
                        label = { Text(character.name) }
                    )
                }
            }

            selectedCharacterIds.sorted().forEach { cid ->
                val character = aiCharacters.firstOrNull { it.id == cid } ?: return@forEach
                OutlinedTextField(
                    value = personaByCharacterId[cid] ?: "",
                    onValueChange = { viewModel.updatePersona(cid, it) },
                    label = { Text("${character.name} · ${stringResource(R.string.town_persona_hint)}") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 扮演角色
            val playOptions = listOf(0L) + selectedCharacterIds.sorted()
            var playMenuExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = playMenuExpanded,
                onExpandedChange = { playMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = if (playCharacterId == 0L) {
                        stringResource(R.string.town_play_as_none)
                    } else {
                        aiCharacters.firstOrNull { it.id == playCharacterId }?.name ?: ""
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.town_play_as)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = playMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = playMenuExpanded,
                    onDismissRequest = { playMenuExpanded = false }
                ) {
                    playOptions.forEach { cid ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (cid == 0L) stringResource(R.string.town_play_as_none)
                                    else aiCharacters.firstOrNull { it.id == cid }?.name ?: ""
                                )
                            },
                            onClick = {
                                viewModel.playCharacterId.value = cid
                                playMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Text(stringResource(R.string.town_locations), style = MaterialTheme.typography.titleMedium)
            locationDrafts.forEachIndexed { index, draft ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = draft.name,
                            onValueChange = { viewModel.updateLocation(index, draft.copy(name = it)) },
                            label = { Text(stringResource(R.string.town_location_name)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.removeLocation(index) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.town_location_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    OutlinedTextField(
                        value = draft.description,
                        onValueChange = { viewModel.updateLocation(index, draft.copy(description = it)) },
                        label = { Text(stringResource(R.string.town_location_description)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            TextButton(onClick = { viewModel.addLocation() }) {
                Text(stringResource(R.string.town_location_add))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                enabled = !saving,
                onClick = {
                    if (name.isBlank() || selectedCharacterIds.isEmpty() || locationDrafts.none { it.name.isNotBlank() }) {
                        localError = invalidMessage
                    } else {
                        viewModel.save(
                            onSuccess = onNavigateBack,
                            onError = { localError = errorPrefix + it }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.town_save_and_generate)) }
        }
    }
}
