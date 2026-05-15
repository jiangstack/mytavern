package org.jiangstack.mytavern.ui.chat

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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.jiangstack.mytavern.MyTavernApplication
import org.jiangstack.mytavern.R
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.ChatSession
import org.jiangstack.mytavern.domain.model.SessionType
import org.jiangstack.mytavern.domain.model.WorldBook

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onNavigateToChat: (Long) -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: ChatListViewModel = viewModel(
        factory = ChatListViewModel.factory(
            container.chatRepository,
            container.characterRepository,
            container.worldBookRepository,
            container.userPreferencesRepository,
            container.sessionCharacterRepository
        )
    )

    val sessions by viewModel.sessions.collectAsState()
    val aiCharacters by viewModel.aiCharacters.collectAsState()
    val worldBooks by viewModel.worldBooks.collectAsState()
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<ChatSession?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chat_list_title)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.chat_list_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        ChatSessionItem(
                            session = session,
                            onClick = { onNavigateToChat(session.id) },
                            onLongClick = {
                                sessionToDelete = session
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateChatDialog(
            aiCharacters = aiCharacters,
            worldBooks = worldBooks,
            onDismiss = { showCreateDialog = false },
            onConfirm = { selectedIds, title, worldBookId ->
                scope.launch {
                    val sessionId = if (selectedIds.size == 1) {
                        viewModel.createSession(selectedIds.first(), title, worldBookId)
                    } else {
                        viewModel.createGroupSession(selectedIds, title, worldBookId)
                    }
                    showCreateDialog = false
                    onNavigateToChat(sessionId)
                }
            }
        )
    }

    if (showDeleteDialog && sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.chat_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.chat_delete_message,
                        sessionToDelete!!.title
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(sessionToDelete!!)
                        showDeleteDialog = false
                        sessionToDelete = null
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
private fun ChatSessionItem(
    session: ChatSession,
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
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (session.type == SessionType.SINGLE) "单聊" else "群聊",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun CreateChatDialog(
    aiCharacters: List<Character>,
    worldBooks: List<WorldBook>,
    onDismiss: () -> Unit,
    onConfirm: (aiCharacterIds: List<Long>, title: String, worldBookId: Long?) -> Unit
) {
    var isGroupChat by remember { mutableStateOf(false) }
    var selectedCharacterIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedWorldBookId by remember { mutableStateOf<Long?>(null) }
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_create_title)) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = {
                            isGroupChat = false
                            selectedCharacterIds = emptySet()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.chat_mode_single),
                            color = if (!isGroupChat) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = {
                            isGroupChat = true
                            selectedCharacterIds = emptySet()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.chat_mode_group),
                            color = if (isGroupChat) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isGroupChat) stringResource(R.string.chat_select_ai_characters)
                    else stringResource(R.string.chat_select_ai_character),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (aiCharacters.isEmpty()) {
                    Text(
                        text = "暂无 AI 角色，请先创建一个",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    aiCharacters.forEach { character ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            if (isGroupChat) {
                                androidx.compose.material3.Checkbox(
                                    checked = character.id in selectedCharacterIds,
                                    onCheckedChange = { checked ->
                                        selectedCharacterIds = if (checked) {
                                            selectedCharacterIds + character.id
                                        } else {
                                            selectedCharacterIds - character.id
                                        }
                                    }
                                )
                            } else {
                                RadioButton(
                                    selected = selectedCharacterIds.contains(character.id),
                                    onClick = { selectedCharacterIds = setOf(character.id) }
                                )
                            }
                            Text(
                                text = character.name,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                if (worldBooks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "选择世界书（可选）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedWorldBookId == null,
                            onClick = { selectedWorldBookId = null }
                        )
                        Text(
                            text = "不使用世界书",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    worldBooks.forEach { worldBook ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedWorldBookId == worldBook.id,
                                onClick = { selectedWorldBookId = worldBook.id }
                            )
                            Text(
                                text = worldBook.name,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("聊天标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedCharacterIds.isNotEmpty()) {
                        onConfirm(
                            selectedCharacterIds.toList(),
                            title.ifBlank { if (isGroupChat) "新群聊" else "新聊天" },
                            selectedWorldBookId
                        )
                    }
                },
                enabled = selectedCharacterIds.isNotEmpty()
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
