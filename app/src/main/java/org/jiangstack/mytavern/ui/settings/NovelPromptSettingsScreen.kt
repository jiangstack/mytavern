package org.jiangstack.mytavern.ui.settings

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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jiangstack.mytavern.MyTavernApplication
import org.jiangstack.mytavern.domain.model.PromptBlockConfig
import org.jiangstack.mytavern.domain.model.PromptBlockDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelPromptSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: NovelPromptSettingsViewModel = viewModel(
        factory = NovelPromptSettingsViewModel.factory(
            container.userPreferencesRepository
        )
    )

    var selectedTab by remember { mutableStateOf(0) }
    val continueBlocks by viewModel.continueBlocks.collectAsState()
    val modifyBlocks by viewModel.modifyBlocks.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var editingBlockIndex by remember { mutableStateOf(-1) }
    var editingContent by remember { mutableStateOf("") }
    var isEditingContinue by remember { mutableStateOf(true) }

    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val currentBlocks = if (selectedTab == 0) continueBlocks else modifyBlocks

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("小说AI提示词设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showResetConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "重置为默认"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("续写提示词") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("修改提示词") }
                )
            }

            LazyColumn(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                itemsIndexed(currentBlocks, key = { _, block -> block.type.name }) { index, block ->
                    PromptBlockItem(
                        block = block,
                        isContinue = selectedTab == 0,
                        canMoveUp = index > 0,
                        canMoveDown = index < currentBlocks.size - 1,
                        onToggle = {
                            if (selectedTab == 0) {
                                viewModel.toggleContinueBlock(index)
                            } else {
                                viewModel.toggleModifyBlock(index)
                            }
                        },
                        onMoveUp = {
                            if (selectedTab == 0) {
                                viewModel.moveContinueBlockUp(index)
                            } else {
                                viewModel.moveModifyBlockUp(index)
                            }
                        },
                        onMoveDown = {
                            if (selectedTab == 0) {
                                viewModel.moveContinueBlockDown(index)
                            } else {
                                viewModel.moveModifyBlockDown(index)
                            }
                        },
                        onEdit = {
                            editingBlockIndex = index
                            editingContent = block.customContent
                                ?: PromptBlockDefaults.defaultContent(
                                    block.type,
                                    selectedTab == 0
                                )
                                ?: ""
                            isEditingContinue = selectedTab == 0
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
            title = { Text(currentBlocks[editingBlockIndex].type.displayName) },
            text = {
                OutlinedTextField(
                    value = editingContent,
                    onValueChange = { editingContent = it },
                    label = { Text("自定义内容，留空则使用默认") },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isEditingContinue) {
                            viewModel.updateContinueBlockContent(editingBlockIndex, editingContent)
                        } else {
                            viewModel.updateModifyBlockContent(editingBlockIndex, editingContent)
                        }
                        showEditDialog = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("确认重置") },
            text = { Text("确定要重置当前提示词配置为默认状态吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedTab == 0) {
                            viewModel.resetContinueBlocks()
                        } else {
                            viewModel.resetModifyBlocks()
                        }
                        showResetConfirmDialog = false
                    }
                ) {
                    Text("重置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun PromptBlockItem(
    block: PromptBlockConfig,
    isContinue: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit
) {
    val hasCustomContent = block.customContent != null
    val displayContent = block.customContent
        ?: PromptBlockDefaults.defaultContent(block.type, isContinue)
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
                            contentDescription = "编辑",
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
                            contentDescription = "上移"
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                if (canMoveDown) {
                    IconButton(onClick = onMoveDown) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "下移"
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
