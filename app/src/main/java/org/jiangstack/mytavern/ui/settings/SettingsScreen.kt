package org.jiangstack.mytavern.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import org.jiangstack.mytavern.domain.model.ApiType
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.LlmConfig
import org.jiangstack.mytavern.domain.model.QuickReply
import org.jiangstack.mytavern.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToHttpLog: () -> Unit = {}
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            container.llmConfigRepository,
            container.characterRepository,
            container.userPreferencesRepository,
            container.quickReplyRepository
        )
    )

    val defaultUserCharacter by viewModel.defaultUserCharacter.collectAsState()
    val userCharacters by viewModel.userCharacters.collectAsState()
    val configs by viewModel.configs.collectAsState()
    val defaultLlmConfigId by viewModel.defaultLlmConfigId.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val chatHistoryCount by viewModel.chatHistoryCount.collectAsState()
    val temperature by viewModel.temperature.collectAsState()
    val maxTokens by viewModel.maxTokens.collectAsState()
    val quickReplies by viewModel.quickReplies.collectAsState()

    var showCharacterPicker by remember { mutableStateOf(false) }
    var showLlmEditDialog by remember { mutableStateOf(false) }
    var editingLlmConfig by remember { mutableStateOf<LlmConfig?>(null) }
    var showLlmDeleteDialog by remember { mutableStateOf(false) }
    var llmConfigToDelete by remember { mutableStateOf<LlmConfig?>(null) }
    var showQuickReplyEditDialog by remember { mutableStateOf(false) }
    var editingQuickReply by remember { mutableStateOf<QuickReply?>(null) }
    var showQuickReplyDeleteDialog by remember { mutableStateOf(false) }
    var quickReplyToDelete by remember { mutableStateOf<QuickReply?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingLlmConfig = null
                showLlmEditDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add LLM Config")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_section_user),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    onClick = { showCharacterPicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text(
                                text = stringResource(R.string.settings_default_user_character),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = defaultUserCharacter?.name
                                    ?: stringResource(R.string.character_not_set),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.settings_section_appearance),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_theme_mode),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ThemeModeSelector(
                            selected = themeMode,
                            onSelect = { viewModel.setThemeMode(it) }
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.settings_section_llm),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (configs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.llm_config_list_empty),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(configs, key = { "llm_${it.id}" }) { config ->
                    val copySuffix = stringResource(R.string.llm_config_copy_suffix)
                    LlmConfigItem(
                        config = config,
                        isDefault = config.id == defaultLlmConfigId,
                        onClick = {
                            editingLlmConfig = config
                            showLlmEditDialog = true
                        },
                        onLongClick = {
                            llmConfigToDelete = config
                            showLlmDeleteDialog = true
                        },
                        onSetDefault = {
                            viewModel.setDefaultLlmConfig(config.id)
                        },
                        onCopy = {
                            viewModel.copyConfig(config, copySuffix)
                        }
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.settings_section_chat),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_chat_history_count),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$chatHistoryCount 条",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.Slider(
                            value = chatHistoryCount.toFloat(),
                            onValueChange = { viewModel.setChatHistoryCount(it.toInt()) },
                            valueRange = 1f..50f,
                            steps = 48
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.settings_temperature),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "%.1f".format(temperature),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.Slider(
                            value = temperature,
                            onValueChange = { viewModel.setTemperature(it) },
                            valueRange = 0.0f..2.0f,
                            steps = 19
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.settings_max_tokens),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$maxTokens",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.Slider(
                            value = maxTokens.toFloat(),
                            onValueChange = { viewModel.setMaxTokens(it.toInt()) },
                            valueRange = 256f..32768f,
                            steps = 31
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_section_quick_reply),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = {
                            editingQuickReply = null
                            showQuickReplyEditDialog = true
                        }
                    ) {
                        Text(stringResource(R.string.quick_reply_add))
                    }
                }
            }

            if (quickReplies.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.quick_reply_list_empty),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(quickReplies, key = { "qr_${it.id}" }) { reply ->
                    QuickReplyItem(
                        reply = reply,
                        onClick = {
                            editingQuickReply = reply
                            showQuickReplyEditDialog = true
                        },
                        onLongClick = {
                            quickReplyToDelete = reply
                            showQuickReplyDeleteDialog = true
                        }
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.settings_section_debug),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    onClick = onNavigateToHttpLog
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.padding(start = 16.dp)) {
                            Text(
                                text = stringResource(R.string.settings_http_log),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCharacterPicker) {
        CharacterPickerDialog(
            characters = userCharacters,
            selected = defaultUserCharacter,
            onSelect = { character ->
                viewModel.setDefaultUserCharacter(character?.id)
                showCharacterPicker = false
            },
            onDismiss = { showCharacterPicker = false }
        )
    }

    if (showLlmEditDialog) {
        LlmConfigEditDialog(
            config = editingLlmConfig,
            onDismiss = { showLlmEditDialog = false },
            onConfirm = { config ->
                viewModel.saveConfig(config)
                showLlmEditDialog = false
            }
        )
    }

    if (showLlmDeleteDialog && llmConfigToDelete != null) {
        AlertDialog(
            onDismissRequest = { showLlmDeleteDialog = false },
            title = { Text(stringResource(R.string.llm_config_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.llm_config_delete_message,
                        llmConfigToDelete!!.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConfig(llmConfigToDelete!!)
                        showLlmDeleteDialog = false
                        llmConfigToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLlmDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showQuickReplyEditDialog) {
        QuickReplyEditDialog(
            reply = editingQuickReply,
            onDismiss = { showQuickReplyEditDialog = false },
            onConfirm = { reply ->
                viewModel.saveQuickReply(reply)
                showQuickReplyEditDialog = false
            }
        )
    }

    if (showQuickReplyDeleteDialog && quickReplyToDelete != null) {
        AlertDialog(
            onDismissRequest = { showQuickReplyDeleteDialog = false },
            title = { Text(stringResource(R.string.quick_reply_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.quick_reply_delete_message,
                        quickReplyToDelete!!.label
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteQuickReply(quickReplyToDelete!!)
                        showQuickReplyDeleteDialog = false
                        quickReplyToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickReplyDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LlmConfigItem(
    config: LlmConfig,
    isDefault: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSetDefault: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${config.apiType.name} | ${config.model}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.llm_config_copy),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isDefault) {
                Text(
                    text = stringResource(R.string.llm_config_default_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                TextButton(onClick = onSetDefault) {
                    Text(stringResource(R.string.llm_config_set_default))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LlmConfigEditDialog(
    config: LlmConfig?,
    onDismiss: () -> Unit,
    onConfirm: (LlmConfig) -> Unit
) {
    var name by remember { mutableStateOf(config?.name ?: "") }
    var apiType by remember { mutableStateOf(config?.apiType ?: ApiType.OPENAI) }
    var baseUrl by remember { mutableStateOf(config?.baseUrl ?: "") }
    var apiKey by remember { mutableStateOf(config?.apiKey ?: "") }
    var model by remember { mutableStateOf(config?.model ?: "") }

    val apiTypes = listOf(ApiType.OPENAI)
    val selectedIndex = apiTypes.indexOf(apiType)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (config == null)
                    stringResource(R.string.llm_config_create_title)
                else
                    stringResource(R.string.llm_config_edit_title)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.llm_config_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.llm_config_api_type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    apiTypes.forEachIndexed { index, type ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = apiTypes.size
                            ),
                            onClick = { apiType = type },
                            selected = index == selectedIndex
                        ) {
                            Text(type.name)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text(stringResource(R.string.llm_config_base_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(stringResource(R.string.llm_config_api_key)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text(stringResource(R.string.llm_config_model)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        LlmConfig(
                            id = config?.id ?: 0,
                            name = name,
                            apiType = apiType,
                            baseUrl = baseUrl,
                            apiKey = apiKey,
                            model = model
                        )
                    )
                },
                enabled = name.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()
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

@Composable
private fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val modes = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)
    val labels = listOf(
        stringResource(R.string.theme_mode_light),
        stringResource(R.string.theme_mode_dark),
        stringResource(R.string.theme_mode_system)
    )
    val selectedIndex = modes.indexOf(selected)

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = modes.size
                ),
                onClick = { onSelect(mode) },
                selected = index == selectedIndex
            ) {
                Text(labels[index])
            }
        }
    }
}

@Composable
private fun CharacterPickerDialog(
    characters: List<Character>,
    selected: Character?,
    onSelect: (Character?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_default_user_character)) },
        text = {
            Column {
                if (characters.isEmpty()) {
                    Text(stringResource(R.string.character_list_empty_user))
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selected == null,
                            onClick = { onSelect(null) }
                        )
                        Text(
                            text = stringResource(R.string.character_not_set),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    characters.forEach { character ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selected?.id == character.id,
                                onClick = { onSelect(character) }
                            )
                            Text(
                                text = character.name,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickReplyItem(
    reply: QuickReply,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
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
                text = reply.label,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = reply.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun QuickReplyEditDialog(
    reply: QuickReply?,
    onDismiss: () -> Unit,
    onConfirm: (QuickReply) -> Unit
) {
    var label by remember { mutableStateOf(reply?.label ?: "") }
    var message by remember { mutableStateOf(reply?.message ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (reply == null)
                    stringResource(R.string.quick_reply_create_title)
                else
                    stringResource(R.string.quick_reply_edit_title)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.quick_reply_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text(stringResource(R.string.quick_reply_message)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        QuickReply(
                            id = reply?.id ?: 0,
                            label = label,
                            message = message
                        )
                    )
                },
                enabled = label.isNotBlank() && message.isNotBlank()
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
