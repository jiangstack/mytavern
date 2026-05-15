package org.jiangstack.mytavern.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jiangstack.mytavern.MyTavernApplication
import org.jiangstack.mytavern.R
import org.jiangstack.mytavern.domain.model.Character
import org.jiangstack.mytavern.domain.model.ChatMessage
import org.jiangstack.mytavern.domain.model.SessionType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(
    sessionId: Long,
    onNavigateBack: () -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: ChatDetailViewModel = viewModel(
        factory = ChatDetailViewModel.factory(
            container.chatRepository,
            container.characterRepository,
            container.worldBookRepository,
            container.sessionCharacterRepository,
            container.llmService,
            container.userPreferencesRepository,
            container.sessionStateRepository,
            sessionId
        )
    )

    val messages by viewModel.messages.collectAsState()
    val session by viewModel.session.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val aiCharacter by viewModel.aiCharacter.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    val streamingReasoning by viewModel.streamingReasoning.collectAsState()
    val thinkingEnabled by viewModel.thinkingEnabled.collectAsState()
    val groupCharacters by viewModel.groupCharacters.collectAsState()
    val activeGeneratingIds by viewModel.activeGeneratingIds.collectAsState()
    val streamingStates by viewModel.streamingStates.collectAsState()
    val sessionStateEnabled by viewModel.sessionStateEnabled.collectAsState()
    val sessionStates by viewModel.sessionStates.collectAsState()

    val worldBooks by container.worldBookRepository.getAllWorldBooks()
        .collectAsState(initial = emptyList())

    var showWorldBookMenu by remember { mutableStateOf(false) }
    var showWorldBookDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<ChatMessage?>(null) }
    var showStateDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    val isSingleChatStreaming = session?.type != SessionType.GROUP &&
        (streamingContent.isNotEmpty() || streamingReasoning.isNotEmpty() || isLoading)
    val isGroupChatStreaming = session?.type == SessionType.GROUP && activeGeneratingIds.isNotEmpty()
    val hasStreaming = isSingleChatStreaming || isGroupChatStreaming
    val itemCount = messages.size +
        (if (isSingleChatStreaming) 1 else 0) +
        (if (isGroupChatStreaming) activeGeneratingIds.size else 0)

    LaunchedEffect(itemCount) {
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        session?.title ?: stringResource(R.string.chat_list_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleThinking() }) {
                        Icon(
                            imageVector = if (thinkingEnabled) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb,
                            contentDescription = if (thinkingEnabled) "思考已开启" else "思考已关闭",
                            tint = if (thinkingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { viewModel.toggleSessionStateEnabled() },
                                onLongClick = { showStateDialog = true }
                            )
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = if (sessionStateEnabled) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (sessionStateEnabled) "状态记录已开启" else "状态记录已关闭",
                            tint = if (sessionStateEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (worldBooks.isNotEmpty()) {
                        IconButton(onClick = { showWorldBookMenu = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "菜单"
                            )
                        }
                        DropdownMenu(
                            expanded = showWorldBookMenu,
                            onDismissRequest = { showWorldBookMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("设置世界书") },
                                onClick = {
                                    showWorldBookMenu = false
                                    showWorldBookDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty() && !hasStreaming) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "开始聊天吧...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                isUser = message.senderId == null,
                                aiName = if (message.senderId == null) "" else (message.senderName ?: aiCharacter?.name ?: "AI"),
                                thinkingEnabled = thinkingEnabled,
                                onRefresh = if (!isLoading && activeGeneratingIds.isEmpty()) {
                                    { viewModel.regenerateMessage(message) }
                                } else null,
                                onEdit = if (!isLoading && activeGeneratingIds.isEmpty()) {
                                    {
                                        editingMessage = message
                                        showEditDialog = true
                                    }
                                } else null,
                                onDelete = if (!isLoading && activeGeneratingIds.isEmpty()) {
                                    {
                                        messageToDelete = message
                                        showDeleteDialog = true
                                    }
                                } else null,
                                onTriggerCharacterReply = if (session?.type == SessionType.GROUP && !isLoading && activeGeneratingIds.isEmpty()) {
                                    { message.senderId?.let { viewModel.triggerCharacterReply(it) } }
                                } else null
                            )
                        }
                        if (isSingleChatStreaming) {
                            item {
                                StreamingMessageBubble(
                                    content = streamingContent,
                                    reasoning = streamingReasoning,
                                    isLoading = isLoading && streamingContent.isEmpty() && streamingReasoning.isEmpty(),
                                    aiName = aiCharacter?.name ?: "AI",
                                    thinkingEnabled = thinkingEnabled
                                )
                            }
                        }
                        if (isGroupChatStreaming) {
                            activeGeneratingIds.forEach { charId ->
                                val charName = groupCharacters.find { it.id == charId }?.name ?: "AI"
                                val state = streamingStates[charId]
                                item(key = "streaming_$charId") {
                                    StreamingMessageBubble(
                                        content = state?.content ?: "",
                                        reasoning = state?.reasoning ?: "",
                                        isLoading = state?.content.isNullOrEmpty() && state?.reasoning.isNullOrEmpty(),
                                        aiName = charName,
                                        thinkingEnabled = thinkingEnabled
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ChatInputBar(
                onSend = { content ->
                    viewModel.sendMessage(content)
                },
                isLoading = isLoading || activeGeneratingIds.isNotEmpty(),
                onCancel = {
                    viewModel.cancelCurrentRequests()
                },
                groupCharacters = groupCharacters,
                isGroupChat = session?.type == SessionType.GROUP
            )
        }
    }

    if (showWorldBookDialog) {
        SelectWorldBookDialog(
            worldBooks = worldBooks,
            currentWorldBookId = session?.worldBookId,
            onDismiss = { showWorldBookDialog = false },
            onConfirm = { worldBookId ->
                viewModel.updateWorldBook(worldBookId)
                showWorldBookDialog = false
            }
        )
    }

    if (showEditDialog && editingMessage != null) {
        EditMessageDialog(
            initialContent = editingMessage!!.content,
            onDismiss = {
                showEditDialog = false
                editingMessage = null
            },
            onConfirm = { newContent ->
                editingMessage?.let { viewModel.editMessage(it, newContent) }
                showEditDialog = false
                editingMessage = null
            }
        )
    }

    if (showDeleteDialog && messageToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                messageToDelete = null
            },
            title = { Text("删除消息") },
            text = { Text("确定要删除这条消息吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        messageToDelete?.let { viewModel.deleteMessage(it) }
                        showDeleteDialog = false
                        messageToDelete = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    messageToDelete = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showStateDialog) {
        SessionStateDialog(
            states = sessionStates,
            onDismiss = { showStateDialog = false },
            onDelete = { key -> viewModel.deleteState(key) }
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isUser: Boolean,
    aiName: String,
    thinkingEnabled: Boolean,
    onRefresh: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onTriggerCharacterReply: (() -> Unit)? = null
) {
    val displayContent = remember(message.content) {
        parseThinkContent(message.content)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onDelete
            ),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Text(
                text = message.senderName ?: aiName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(start = 8.dp, bottom = 2.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { onTriggerCharacterReply?.invoke() })
                    }
            )
        }

        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(horizontal = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (thinkingEnabled && displayContent.reasoning.isNotEmpty()) {
                    ThinkBlock(reasoning = displayContent.reasoning)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = displayContent.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (!isUser && (message.promptTokens != null || message.completionTokens != null || message.totalTokens != null)) {
            Text(
                text = buildString {
                    message.promptTokens?.let { append("提示: $it ") }
                    message.completionTokens?.let { append("补全: $it ") }
                    message.totalTokens?.let { append("总计: $it") }
                }.trim(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
            )
        }

        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isUser && onRefresh != null) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "重新生成",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isUser && onEdit != null) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "编辑",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamingMessageBubble(
    content: String,
    reasoning: String,
    isLoading: Boolean,
    aiName: String,
    thinkingEnabled: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = aiName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
        )

        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(horizontal = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 4.dp,
                bottomEnd = 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (thinkingEnabled && reasoning.isNotEmpty()) {
                    ThinkBlock(reasoning = reasoning)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (content.isNotEmpty()) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkBlock(reasoning: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "思考",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = reasoning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

private data class ParsedContent(
    val reasoning: String,
    val content: String
)

private fun parseThinkContent(raw: String): ParsedContent {
    val thinkStart = raw.indexOf("<think>")
    if (thinkStart == -1) {
        return ParsedContent(reasoning = "", content = raw)
    }
    val thinkEnd = raw.indexOf("</think>")
    if (thinkEnd == -1) {
        return ParsedContent(reasoning = "", content = raw)
    }
    val reasoning = raw.substring(thinkStart + 7, thinkEnd).trim()
    val content = raw.substring(thinkEnd + 8).trim()
    return ParsedContent(reasoning = reasoning, content = content)
}

@Composable
private fun ChatInputBar(
    onSend: (String) -> Unit,
    isLoading: Boolean,
    onCancel: () -> Unit,
    groupCharacters: List<Character> = emptyList(),
    isGroupChat: Boolean = false
) {
    var text by remember { mutableStateOf("") }
    var showMentionPicker by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isGroupChat && groupCharacters.isNotEmpty()) {
            IconButton(
                onClick = { showMentionPicker = true },
                enabled = !isLoading
            ) {
                Text(
                    text = "@",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (!isLoading) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(stringResource(R.string.chat_input_hint)) },
            modifier = Modifier.weight(1f),
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (text.isNotBlank() && !isLoading) {
                        onSend(text)
                        text = ""
                        keyboardController?.hide()
                    }
                }
            ),
            maxLines = 4
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = {
                if (isLoading) {
                    onCancel()
                } else if (text.isNotBlank()) {
                    onSend(text)
                    text = ""
                    keyboardController?.hide()
                }
            },
            enabled = isLoading || text.isNotBlank()
        ) {
            Icon(
                if (isLoading) Icons.Filled.Close else Icons.AutoMirrored.Filled.Send,
                contentDescription = if (isLoading) "取消" else stringResource(R.string.chat_send)
            )
        }
    }

    if (showMentionPicker) {
        MentionPickerDialog(
            characters = groupCharacters,
            onSelect = { charName ->
                text = "$text@$charName "
                showMentionPicker = false
            },
            onDismiss = { showMentionPicker = false }
        )
    }
}

@Composable
private fun SelectWorldBookDialog(
    worldBooks: List<org.jiangstack.mytavern.domain.model.WorldBook>,
    currentWorldBookId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (worldBookId: Long?) -> Unit
) {
    var selectedWorldBookId by remember { mutableStateOf(currentWorldBookId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置世界书") },
        text = {
            Column {
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
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedWorldBookId) }
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

@Composable
private fun MentionPickerDialog(
    characters: List<Character>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_mention_title)) },
        text = {
            Column {
                if (characters.isEmpty()) {
                    Text(
                        text = stringResource(R.string.chat_mention_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    characters.forEach { character ->
                        TextButton(
                            onClick = { onSelect(character.name) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(character.name)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun EditMessageDialog(
    initialContent: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑消息") },
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 6
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(content) },
                enabled = content.isNotBlank()
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

@Composable
private fun SessionStateDialog(
    states: List<org.jiangstack.mytavern.domain.model.SessionState>,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("会话状态") },
        text = {
            if (states.isEmpty()) {
                Text(
                    text = "暂无记录的状态",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column {
                    states.forEach { state ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.key,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = state.value,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            IconButton(
                                onClick = { onDelete(state.key) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "删除",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
