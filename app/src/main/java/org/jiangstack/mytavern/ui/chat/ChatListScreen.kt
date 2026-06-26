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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.SmartToy
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
import androidx.compose.runtime.mutableIntStateOf
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
import org.jiangstack.mytavern.domain.model.Novel
import org.jiangstack.mytavern.domain.model.SessionType
import org.jiangstack.mytavern.domain.model.WorldBook
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onNavigateToChat: (Long) -> Unit,
    onNavigateToAgentChat: (Long) -> Unit = onNavigateToChat
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: ChatListViewModel = viewModel(
        factory = ChatListViewModel.factory(
            container.chatRepository,
            container.characterRepository,
            container.worldBookRepository,
            container.novelRepository,
            container.userPreferencesRepository,
            container.sessionCharacterRepository
        )
    )

    val sessionItems by viewModel.sessionItems.collectAsState()
    val aiCharacters by viewModel.aiCharacters.collectAsState()
    val worldBooks by viewModel.worldBooks.collectAsState()
    val novels by viewModel.novels.collectAsState()
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<ChatSession?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var sessionToEdit by remember { mutableStateOf<ChatSession?>(null) }
    var editTitle by remember { mutableStateOf("") }

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
            if (sessionItems.isEmpty()) {
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
                    items(sessionItems, key = { it.session.id }) { item ->
                        ChatSessionItem(
                            item = item,
                            onClick = {
                                if (item.session.type == SessionType.AGENT) {
                                    onNavigateToAgentChat(item.session.id)
                                } else {
                                    onNavigateToChat(item.session.id)
                                }
                            },
                            onLongClick = {
                                sessionToDelete = item.session
                                showDeleteDialog = true
                            },
                            onEditClick = {
                                sessionToEdit = item.session
                                editTitle = item.session.title
                                showEditDialog = true
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
            novels = novels,
            onDismiss = { showCreateDialog = false },
            onConfirmChat = { selectedIds, title, worldBookId ->
                scope.launch {
                    val sessionId = if (selectedIds.size == 1) {
                        viewModel.createSession(selectedIds.first(), title, worldBookId)
                    } else {
                        viewModel.createGroupSession(selectedIds, title, worldBookId)
                    }
                    showCreateDialog = false
                    onNavigateToChat(sessionId)
                }
            },
            onConfirmAgent = { novelId, title, systemPrompt ->
                scope.launch {
                    val sessionId = viewModel.createAgentSession(novelId, title, systemPrompt)
                    showCreateDialog = false
                    onNavigateToAgentChat(sessionId)
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

    if (showEditDialog && sessionToEdit != null) {
        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
                sessionToEdit = null
            },
            title = { Text(stringResource(R.string.chat_edit_title)) },
            text = {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text(stringResource(R.string.chat_title_label)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateSessionTitle(sessionToEdit!!, editTitle)
                        showEditDialog = false
                        sessionToEdit = null
                    },
                    enabled = editTitle.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditDialog = false
                    sessionToEdit = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatSessionItem(
    item: ChatSessionListItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val session = item.session
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
            ChatSessionAvatar(item = item)

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.chat_edit_title),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (session.type) {
                        SessionType.SINGLE -> "单聊"
                        SessionType.GROUP -> "群聊"
                        SessionType.AGENT -> "智能体"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun ChatSessionAvatar(item: ChatSessionListItem) {
    val avatarUri = item.avatarUri
    if (avatarUri != null) {
        AsyncImage(
            model = avatarUri,
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
                imageVector = if (item.session.type == SessionType.AGENT)
                    Icons.Default.SmartToy else Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        }
    }
    Spacer(modifier = Modifier.width(16.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateChatDialog(
    aiCharacters: List<Character>,
    worldBooks: List<WorldBook>,
    novels: List<Novel>,
    onDismiss: () -> Unit,
    onConfirmChat: (aiCharacterIds: List<Long>, title: String, worldBookId: Long?) -> Unit,
    onConfirmAgent: (novelId: Long, title: String, systemPrompt: String?) -> Unit
) {
    var chatMode by remember { mutableIntStateOf(0) } // 0=单聊, 1=群聊, 2=智能体
    var selectedCharacterIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedWorldBookId by remember { mutableStateOf<Long?>(null) }
    var selectedNovelId by remember { mutableStateOf<Long?>(null) }
    var title by remember { mutableStateOf("") }
    var agentSystemPrompt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_create_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = chatMode == 0,
                        onClick = {
                            chatMode = 0
                            selectedCharacterIds = emptySet()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text(stringResource(R.string.chat_mode_single))
                    }
                    SegmentedButton(
                        selected = chatMode == 1,
                        onClick = {
                            chatMode = 1
                            selectedCharacterIds = emptySet()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text(stringResource(R.string.chat_mode_group))
                    }
                    SegmentedButton(
                        selected = chatMode == 2,
                        onClick = {
                            chatMode = 2
                            selectedCharacterIds = emptySet()
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text(stringResource(R.string.chat_mode_agent))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (chatMode == 2) {
                    // 智能体模式：选择小说
                    Text(
                        text = stringResource(R.string.chat_select_novel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (novels.isEmpty()) {
                        Text(
                            text = stringResource(R.string.chat_no_novels),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        novels.forEach { novel ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedNovelId == novel.id,
                                    onClick = {
                                        selectedNovelId = novel.id
                                        if (title.isBlank()) {
                                            title = "小说助手 - ${novel.title}"
                                        }
                                    }
                                )
                                Text(
                                    text = novel.title,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.chat_agent_system_prompt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = agentSystemPrompt,
                        onValueChange = { agentSystemPrompt = it },
                        label = { Text(stringResource(R.string.chat_agent_system_prompt_hint)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        maxLines = 6
                    )
                } else {
                    // 单聊/群聊模式：选择 AI 角色
                    Text(
                        text = if (chatMode == 1) stringResource(R.string.chat_select_ai_characters)
                        else stringResource(R.string.chat_select_ai_character),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (aiCharacters.isEmpty()) {
                        Text(
                            text = stringResource(R.string.chat_no_ai_characters),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        aiCharacters.forEach { character ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                if (chatMode == 1) {
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
                            text = stringResource(R.string.chat_select_world_book),
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
                                text = stringResource(R.string.chat_no_world_book),
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
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.chat_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (chatMode) {
                        0, 1 -> {
                            if (selectedCharacterIds.isNotEmpty()) {
                                onConfirmChat(
                                    selectedCharacterIds.toList(),
                                    title.ifBlank { if (chatMode == 1) "新群聊" else "新聊天" },
                                    selectedWorldBookId
                                )
                            }
                        }
                        2 -> {
                            selectedNovelId?.let { novelId ->
                                onConfirmAgent(
                                    novelId,
                                    title.ifBlank { "小说助手" },
                                    agentSystemPrompt.ifBlank { null }
                                )
                            }
                        }
                    }
                },
                enabled = when (chatMode) {
                    0, 1 -> selectedCharacterIds.isNotEmpty()
                    2 -> selectedNovelId != null
                    else -> false
                }
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
