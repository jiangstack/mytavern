package org.jiangstack.mytavern.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jiangstack.mytavern.MyTavernApplication
import org.jiangstack.mytavern.R
import org.jiangstack.mytavern.domain.model.QuickReply

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickReplySettingsScreen(
    onNavigateBack: () -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            container.llmConfigRepository,
            container.characterRepository,
            container.userPreferencesRepository,
            container.quickReplyRepository,
            container.backupRepository
        )
    )

    val quickReplies by viewModel.quickReplies.collectAsState()

    var showQuickReplyEditDialog by remember { mutableStateOf(false) }
    var editingQuickReply by remember { mutableStateOf<QuickReply?>(null) }
    var showQuickReplyDeleteDialog by remember { mutableStateOf(false) }
    var quickReplyToDelete by remember { mutableStateOf<QuickReply?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_section_quick_reply)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            editingQuickReply = null
                            showQuickReplyEditDialog = true
                        }
                    ) {
                        Text(stringResource(R.string.quick_reply_add))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp)
        ) {
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
        }
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
