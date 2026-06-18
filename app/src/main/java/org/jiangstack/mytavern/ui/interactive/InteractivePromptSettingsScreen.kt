package org.jiangstack.mytavern.ui.interactive

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import org.jiangstack.mytavern.domain.model.PromptBlockConfig
import org.jiangstack.mytavern.domain.model.PromptBlockDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractivePromptSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: InteractivePromptSettingsViewModel = viewModel(
        factory = InteractivePromptSettingsViewModel.factory(
            container.userPreferencesRepository
        )
    )

    val blocks by viewModel.blocks.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var editingBlockIndex by remember { mutableStateOf(-1) }
    var editingContent by remember { mutableStateOf("") }

    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.interactive_prompt_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showResetConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.interactive_prompt_settings)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                itemsIndexed(blocks, key = { _, block -> block.type.name }) { index, block ->
                    PromptBlockItem(
                        block = block,
                        canMoveUp = index > 0,
                        canMoveDown = index < blocks.size - 1,
                        onToggle = { viewModel.toggleBlock(index) },
                        onMoveUp = { viewModel.moveBlockUp(index) },
                        onMoveDown = { viewModel.moveBlockDown(index) },
                        onEdit = {
                            editingBlockIndex = index
                            editingContent = block.customContent
                                ?: PromptBlockDefaults.defaultContent(block.type)
                                ?: ""
                            showEditDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showEditDialog && editingBlockIndex >= 0) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(blocks[editingBlockIndex].type.displayName) },
            text = {
                OutlinedTextField(
                    value = editingContent,
                    onValueChange = { editingContent = it },
                    label = { Text(stringResource(R.string.chat_agent_system_prompt_hint)) },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateBlockContent(editingBlockIndex, editingContent)
                        showEditDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text(stringResource(R.string.interactive_clear_story_title)) },
            text = { Text(stringResource(R.string.interactive_prompt_reset_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetBlocks()
                        showResetConfirmDialog = false
                    }
                ) {
                    Text(stringResource(R.string.interactive_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun PromptBlockItem(
    block: PromptBlockConfig,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit
) {
    val hasCustomContent = block.customContent != null
    val displayContent = block.customContent
        ?: PromptBlockDefaults.defaultContent(block.type)
        ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (block.isEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = block.type.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (block.isEnabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        }
                    )
                    if (hasCustomContent && block.type.editable) {
                        Text(
                            text = displayContent.take(40) + if (displayContent.length > 40) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }

                if (block.type.editable) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.interactive_prompt_settings),
                            tint = if (hasCustomContent) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                if (canMoveUp) {
                    IconButton(onClick = onMoveUp) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.interactive_prompt_settings)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                if (canMoveDown) {
                    IconButton(onClick = onMoveDown) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.interactive_prompt_settings)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Switch(
                    checked = block.isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}
