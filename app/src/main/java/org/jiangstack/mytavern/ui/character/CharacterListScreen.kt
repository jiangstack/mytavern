package org.jiangstack.mytavern.ui.character

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import org.jiangstack.mytavern.MyTavernApplication
import org.jiangstack.mytavern.R
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.CharacterType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    onNavigateToDetail: (Long) -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: CharacterListViewModel = viewModel(
        factory = CharacterListViewModel.factory(
            container.characterRepository,
            container.userPreferencesRepository,
            container.llmService
        )
    )

    val characters by viewModel.characters.collectAsState()
    val defaultUserCharacterId by viewModel.defaultUserCharacterId.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var editingCharacter by remember { mutableStateOf<Character?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var characterToDelete by remember { mutableStateOf<Character?>(null) }
    var editAvatarUri by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            editAvatarUri = it.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.character_list_title)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingCharacter = null
                editAvatarUri = null
                showEditDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Character")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (characters.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.character_list_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(characters, key = { it.id }) { character ->
                        CharacterItem(
                            character = character,
                            isDefaultUser = character.type == CharacterType.USER &&
                                    character.id == defaultUserCharacterId,
                            onClick = {
                                editingCharacter = character
                                editAvatarUri = character.avatarUri
                                showEditDialog = true
                            },
                            onLongClick = {
                                characterToDelete = character
                                showDeleteDialog = true
                            },
                            onSetDefault = {
                                viewModel.setDefaultUserCharacter(character.id)
                            }
                        )
                    }
                }
            }
        }
    }

    val isGenerating by viewModel.isGenerating.collectAsState()
    val aiGeneratedName by viewModel.aiGeneratedName.collectAsState()
    val aiGeneratedDescription by viewModel.aiGeneratedDescription.collectAsState()
    val generateError by viewModel.generateError.collectAsState()

    if (showEditDialog) {
        CharacterEditDialog(
            character = editingCharacter,
            avatarUri = editAvatarUri,
            onPickAvatar = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDismiss = {
                showEditDialog = false
                viewModel.resetGeneratedFields()
                viewModel.clearGenerateError()
            },
            onConfirm = { character ->
                viewModel.saveCharacter(character)
                showEditDialog = false
                viewModel.resetGeneratedFields()
                viewModel.clearGenerateError()
            },
            isGenerating = isGenerating,
            aiGeneratedName = aiGeneratedName,
            aiGeneratedDescription = aiGeneratedDescription,
            generateError = generateError,
            onGenerate = { prompt -> viewModel.generateCharacter(prompt) },
            onClearGenerateError = { viewModel.clearGenerateError() }
        )
    }

    if (showDeleteDialog && characterToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.character_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.character_delete_message,
                        characterToDelete!!.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCharacter(characterToDelete!!)
                        showDeleteDialog = false
                        characterToDelete = null
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
private fun CharacterItem(
    character: Character,
    isDefaultUser: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSetDefault: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            if (character.avatarUri != null) {
                AsyncImage(
                    model = character.avatarUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (character.type == CharacterType.USER)
                            Icons.Default.Person else Icons.Default.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = character.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val typeLabel = if (character.type == CharacterType.USER)
                        stringResource(R.string.character_type_user)
                    else
                        stringResource(R.string.character_type_ai)
                    val typeColor = if (character.type == CharacterType.USER)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary

                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = typeColor
                    )

                    if (isDefaultUser) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.character_default_label),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.character_default_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Actions
            if (character.type == CharacterType.USER && !isDefaultUser) {
                TextButton(onClick = onSetDefault) {
                    Text(stringResource(R.string.character_set_default))
                }
            }
        }
    }
}

@Composable
private fun CharacterEditDialog(
    character: Character?,
    avatarUri: String?,
    onPickAvatar: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Character) -> Unit,
    isGenerating: Boolean = false,
    aiGeneratedName: String = "",
    aiGeneratedDescription: String = "",
    generateError: String? = null,
    onGenerate: (String) -> Unit = {},
    onClearGenerateError: () -> Unit = {}
) {
    var name by remember { mutableStateOf(character?.name ?: "") }
    var description by remember { mutableStateOf(character?.description ?: "") }
    var type by remember { mutableStateOf(character?.type ?: CharacterType.AI) }
    var aiPrompt by remember { mutableStateOf("") }

    // 当 AI 生成了新的内容时，自动填充
    LaunchedEffect(aiGeneratedName, aiGeneratedDescription) {
        if (aiGeneratedName.isNotBlank()) {
            name = aiGeneratedName
        }
        if (aiGeneratedDescription.isNotBlank()) {
            description = aiGeneratedDescription
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (character == null)
                    stringResource(R.string.character_create_title)
                else
                    stringResource(R.string.character_edit_title)
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPickAvatar),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = stringResource(R.string.character_select_avatar),
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(R.string.character_select_avatar),
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.character_select_avatar),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // AI 生成区域（仅在创建新角色时显示）
                if (character == null) {
                    OutlinedTextField(
                        value = aiPrompt,
                        onValueChange = { aiPrompt = it },
                        label = { Text(stringResource(R.string.character_ai_generate_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isGenerating
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onGenerate(aiPrompt) },
                        enabled = aiPrompt.isNotBlank() && !isGenerating,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.character_ai_generating))
                        } else {
                            Text(stringResource(R.string.character_ai_generate))
                        }
                    }

                    // 显示错误信息
                    if (generateError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = generateError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.character_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.character_description)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == CharacterType.USER,
                        onClick = { type = CharacterType.USER },
                        label = { Text(stringResource(R.string.character_type_user)) }
                    )
                    FilterChip(
                        selected = type == CharacterType.AI,
                        onClick = { type = CharacterType.AI },
                        label = { Text(stringResource(R.string.character_type_ai)) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        Character(
                            id = character?.id ?: 0,
                            name = name,
                            description = description,
                            avatarUri = avatarUri,
                            type = type
                        )
                    )
                },
                enabled = name.isNotBlank()
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
