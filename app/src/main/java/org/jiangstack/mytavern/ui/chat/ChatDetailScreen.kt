package org.jiangstack.mytavern.ui.chat

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
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jiangstack.mytavern.MyTavernApplication
import org.jiangstack.mytavern.R
import org.jiangstack.mytavern.domain.model.ChatMessage

@OptIn(ExperimentalMaterial3Api::class)
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
            container.llmService,
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

    val worldBooks by container.worldBookRepository.getAllWorldBooks()
        .collectAsState(initial = emptyList())

    var showWorldBookMenu by remember { mutableStateOf(false) }
    var showWorldBookDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    val hasStreaming = streamingContent.isNotEmpty() || streamingReasoning.isNotEmpty() || isLoading
    val itemCount = messages.size + if (hasStreaming) 1 else 0

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
                                aiName = aiCharacter?.name ?: "AI",
                                thinkingEnabled = thinkingEnabled
                            )
                        }
                        if (hasStreaming) {
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
                    }
                }
            }

            ChatInputBar(
                onSend = { content ->
                    viewModel.sendMessage(content)
                },
                isLoading = isLoading,
                onCancel = {
                    viewModel.cancelCurrentRequest()
                }
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
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isUser: Boolean,
    aiName: String,
    thinkingEnabled: Boolean
) {
    val displayContent = remember(message.content) {
        parseThinkContent(message.content)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
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
                if (!isUser) {
                    Text(
                        text = message.senderName ?: aiName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
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
                Text(
                    text = aiName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))

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
                        modifier = Modifier.padding(top = 8.dp),
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
    onCancel: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
