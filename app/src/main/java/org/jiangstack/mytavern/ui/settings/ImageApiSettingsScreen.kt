package org.jiangstack.mytavern.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import org.jiangstack.mytavern.domain.model.ImageApiConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageApiSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            container.llmConfigRepository,
            container.imageApiConfigRepository,
            container.characterRepository,
            container.userPreferencesRepository,
            container.quickReplyRepository,
            container.backupRepository
        )
    )

    val configs by viewModel.imageApiConfigs.collectAsState()
    val defaultImageApiConfigId by viewModel.defaultImageApiConfigId.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<ImageApiConfig?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var configToDelete by remember { mutableStateOf<ImageApiConfig?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.image_api_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingConfig = null
                showEditDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Image API Config")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (configs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.image_api_config_list_empty),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(configs, key = { "image_api_${it.id}" }) { config ->
                    val copySuffix = stringResource(R.string.llm_config_copy_suffix)
                    ImageApiConfigItem(
                        config = config,
                        isDefault = config.id == defaultImageApiConfigId,
                        onClick = {
                            editingConfig = config
                            showEditDialog = true
                        },
                        onLongClick = {
                            configToDelete = config
                            showDeleteDialog = true
                        },
                        onSetDefault = {
                            viewModel.setDefaultImageApiConfig(config.id)
                        },
                        onCopy = {
                            viewModel.copyImageApiConfig(config, copySuffix)
                        }
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        ImageApiConfigEditDialog(
            config = editingConfig,
            onDismiss = { showEditDialog = false },
            onConfirm = { config ->
                viewModel.saveImageApiConfig(config)
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog && configToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.image_api_config_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.image_api_config_delete_message,
                        configToDelete!!.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteImageApiConfig(configToDelete!!)
                        showDeleteDialog = false
                        configToDelete = null
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
private fun ImageApiConfigItem(
    config: ImageApiConfig,
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
                    text = config.model,
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
                    text = stringResource(R.string.image_api_config_default_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                TextButton(onClick = onSetDefault) {
                    Text(stringResource(R.string.image_api_config_set_default))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageApiConfigEditDialog(
    config: ImageApiConfig?,
    onDismiss: () -> Unit,
    onConfirm: (ImageApiConfig) -> Unit
) {
    var name by remember { mutableStateOf(config?.name ?: "") }
    var baseUrl by remember { mutableStateOf(config?.baseUrl ?: "https://api.kie.ai/api/v1/jobs") }
    var apiKey by remember { mutableStateOf(config?.apiKey ?: "") }
    var model by remember { mutableStateOf(config?.model ?: "grok-imagine/text-to-image") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (config == null)
                    stringResource(R.string.image_api_config_create_title)
                else
                    stringResource(R.string.image_api_config_edit_title)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.image_api_config_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text(stringResource(R.string.image_api_config_base_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(stringResource(R.string.image_api_config_api_key)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text(stringResource(R.string.image_api_config_model)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        ImageApiConfig(
                            id = config?.id ?: 0,
                            name = name,
                            baseUrl = baseUrl,
                            apiKey = apiKey,
                            model = model
                        )
                    )
                },
                enabled = name.isNotBlank() && baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
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
