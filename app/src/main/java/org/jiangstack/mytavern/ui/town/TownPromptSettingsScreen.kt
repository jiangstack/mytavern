package org.jiangstack.mytavern.ui.town

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
fun TownPromptSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: TownPromptSettingsViewModel = viewModel(
        factory = TownPromptSettingsViewModel.factory(container.userPreferencesRepository)
    )

    val blocks by viewModel.blocks.collectAsState()
    val maxIterations by viewModel.maxIterations.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var editingBlockIndex by remember { mutableStateOf(-1) }
    var editingContent by remember { mutableStateOf("") }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.town_prompt_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(android.R.string.cancel))
                    }
                },
                actions = {
                    IconButton(onClick = { showResetConfirmDialog = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.town_prompt_settings_title))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // 最大迭代次数
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_town_max_iterations),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                var iterationsMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = iterationsMenuExpanded,
                    onExpandedChange = { iterationsMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = maxIterations.toString(),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = iterationsMenuExpanded) },
                        singleLine = true,
                        modifier = Modifier
                            .width(96.dp)
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = iterationsMenuExpanded,
                        onDismissRequest = { iterationsMenuExpanded = false }
                    ) {
                        (1..5).forEach { n ->
                            DropdownMenuItem(
                                text = { Text(n.toString()) },
                                onClick = {
                                    viewModel.setMaxIterations(n)
                                    iterationsMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            LazyColumn {
                itemsIndexed(blocks, key = { _, block -> block.type.name }) { index, block ->
                    TownPromptBlockItem(
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

    if (showEditDialog && editingBlockIndex in blocks.indices) {
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
                TextButton(onClick = {
                    viewModel.updateBlockContent(editingBlockIndex, editingContent)
                    showEditDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text(stringResource(R.string.town_prompt_settings_title)) },
            text = { Text(stringResource(R.string.town_prompt_reset_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetBlocks()
                    showResetConfirmDialog = false
                }) { Text(stringResource(R.string.town_snapshot_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun TownPromptBlockItem(
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
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.town_prompt_settings_title),
                            tint = if (hasCustomContent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (canMoveUp) {
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                if (canMoveDown) {
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Switch(checked = block.isEnabled, onCheckedChange = { onToggle() })
            }
        }
    }
}
