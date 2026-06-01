package org.jiangstack.mytavern.ui.worldbook

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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import org.jiangstack.mytavern.domain.model.WorldBook

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldBookListScreen(
    onNavigateToDetail: (Long) -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: WorldBookListViewModel = viewModel(
        factory = WorldBookListViewModel.factory(container.worldBookRepository)
    )

    val worldBooks by viewModel.worldBooks.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var editingWorldBook by remember { mutableStateOf<WorldBook?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var worldBookToDelete by remember { mutableStateOf<WorldBook?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuWorldBook by remember { mutableStateOf<WorldBook?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.worldbook_list_title)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingWorldBook = null
                showEditDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add World Book")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (worldBooks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.worldbook_list_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(worldBooks, key = { it.id }) { worldBook ->
                        WorldBookItem(
                            worldBook = worldBook,
                            onClick = { onNavigateToDetail(worldBook.id) },
                            onLongClick = {
                                contextMenuWorldBook = worldBook
                                showContextMenu = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showContextMenu && contextMenuWorldBook != null) {
        AlertDialog(
            onDismissRequest = { showContextMenu = false },
            title = { Text(contextMenuWorldBook!!.name) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            editingWorldBook = contextMenuWorldBook
                            showEditDialog = true
                            showContextMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.worldbook_edit))
                    }
                    TextButton(
                        onClick = {
                            viewModel.copyWorldBook(contextMenuWorldBook!!.id)
                            showContextMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.worldbook_copy))
                    }
                    TextButton(
                        onClick = {
                            worldBookToDelete = contextMenuWorldBook
                            showDeleteDialog = true
                            showContextMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            stringResource(R.string.worldbook_delete_title),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showContextMenu = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showEditDialog) {
        WorldBookEditDialog(
            worldBook = editingWorldBook,
            onDismiss = { showEditDialog = false },
            onConfirm = { worldBook ->
                viewModel.saveWorldBook(worldBook)
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog && worldBookToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.worldbook_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.worldbook_delete_message,
                        worldBookToDelete!!.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteWorldBook(worldBookToDelete!!)
                        showDeleteDialog = false
                        worldBookToDelete = null
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
private fun WorldBookItem(
    worldBook: WorldBook,
    onClick: () -> Unit,
    onLongClick: () -> Unit
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
            Icon(
                imageVector = Icons.Default.Book,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = worldBook.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = worldBook.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                if (worldBook.rules.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${worldBook.rules.size} ${stringResource(R.string.worldbook_rules_title)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun WorldBookEditDialog(
    worldBook: WorldBook?,
    onDismiss: () -> Unit,
    onConfirm: (WorldBook) -> Unit
) {
    var name by remember { mutableStateOf(worldBook?.name ?: "") }
    var description by remember { mutableStateOf(worldBook?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (worldBook == null)
                    stringResource(R.string.worldbook_create_title)
                else
                    stringResource(R.string.worldbook_edit_title)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.worldbook_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.worldbook_description)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        WorldBook(
                            id = worldBook?.id ?: 0,
                            name = name,
                            description = description
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
