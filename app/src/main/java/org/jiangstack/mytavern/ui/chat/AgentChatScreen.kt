package org.jiangstack.mytavern.ui.chat

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import org.jiangstack.mytavern.MyTavernApplication
import org.jiangstack.mytavern.R
import org.jiangstack.mytavern.domain.model.ChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentChatScreen(
    sessionId: Long,
    onNavigateBack: () -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: AgentChatViewModel = viewModel(
        factory = AgentChatViewModel.factory(
            container.chatRepository,
            container.novelRepository,
            container.userPreferencesRepository,
            container.novelAgentService,
            sessionId
        )
    )

    val messages by viewModel.messages.collectAsState()
    val session by viewModel.session.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val streamingContent by viewModel.streamingContent.collectAsState()
    val streamingThinking by viewModel.streamingThinking.collectAsState()
    val agentSteps by viewModel.agentSteps.collectAsState()
    val pendingApproval by viewModel.pendingApproval.collectAsState()
    val currentIteration by viewModel.currentIteration.collectAsState()
    val thinkingEnabled by viewModel.thinkingEnabled.collectAsState()

    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    var showMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.updateBackgroundUri(it.toString())
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    val isStillThinking = streamingThinking.isNotEmpty() && streamingContent.isEmpty()
    val hasStreaming = streamingContent.isNotEmpty() || isStillThinking || isLoading
    val itemCount = messages.size + agentSteps.size + (if (hasStreaming) 1 else 0) +
        (if (pendingApproval != null) 1 else 0)

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
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(session?.title ?: "智能体")
                        if (isLoading) {
                            Text(
                                text = "运行中... 第${currentIteration}轮",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleThinkingEnabled() }) {
                        Icon(
                            imageVector = if (thinkingEnabled) Icons.Default.Lightbulb else Icons.Outlined.Lightbulb,
                            contentDescription = "思考模式",
                            tint = if (thinkingEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "更多"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("设置背景图片") },
                                onClick = {
                                    showMenu = false
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            )
                            if (session?.backgroundUri != null) {
                                DropdownMenuItem(
                                    text = { Text("清除背景") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.updateBackgroundUri(null)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("清空消息") },
                                onClick = {
                                    showMenu = false
                                    showClearDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            session?.backgroundUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.2f
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                    when (message.messageType) {
                        "agent_thinking" -> AgentStepBubble(
                            step = AgentStep(type = "thinking", content = message.content)
                        )
                        "tool_call" -> AgentStepBubble(
                            step = AgentStep(type = "tool_call", content = message.content, toolName = extractToolName(message.content))
                        )
                        "tool_result" -> AgentStepBubble(
                            step = AgentStep(type = "tool_result", content = message.content)
                        )
                        else -> AgentMessageBubble(message = message)
                    }
                }

                // 只显示当前运行时的临时步骤（如果有的话）
                if (isLoading) {
                    items(agentSteps) { step ->
                        AgentStepBubble(step = step)
                    }
                }

                if (hasStreaming) {
                    item {
                        AgentStreamingBubble(
                            thinking = if (isStillThinking) streamingThinking else "",
                            content = streamingContent
                        )
                    }
                }

                pendingApproval?.let { approval ->
                    item {
                        WriteApprovalCard(
                            chapterNumber = approval.chapterNumber,
                            title = approval.title,
                            content = approval.content,
                            onApprove = {
                                approval.onApprove()
                                keyboardController?.hide()
                            },
                            onReject = { approval.onReject() }
                        )
                    }
                }
            }
            }

            AgentInputBar(
                inputText = inputText,
                onInputChange = { inputText = it },
                onSend = {
                    if (inputText.text.isNotBlank()) {
                        viewModel.sendMessage(inputText.text)
                        inputText = TextFieldValue("")
                        keyboardController?.hide()
                    }
                },
                onCancel = { viewModel.cancelCurrentRequest() },
                isLoading = isLoading,
                enabled = pendingApproval == null
            )
        }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空消息") },
            text = { Text("确定要清空所有消息吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearMessages()
                        showClearDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun AgentMessageBubble(message: ChatMessage) {
    val isUser = message.role != "assistant"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(32.dp)
                    .padding(end = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AgentStepBubble(step: AgentStep) {
    var expanded by remember { mutableStateOf(false) }
    val isThinking = step.type == "thinking"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (step.type) {
                    "thinking" -> MaterialTheme.colorScheme.tertiaryContainer
                    "tool_call" -> MaterialTheme.colorScheme.secondaryContainer
                    "tool_result" -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            onClick = { if (isThinking) expanded = !expanded }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = when (step.type) {
                            "thinking" -> Icons.Default.Lightbulb
                            "tool_call" -> Icons.Default.Build
                            "tool_result" -> Icons.Default.Check
                            else -> Icons.Default.SmartToy
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (step.type) {
                            "thinking" -> "思考"
                            "tool_call" -> "调用工具"
                            "tool_result" -> "工具结果"
                            else -> "步骤"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (step.toolName != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = step.toolName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (isThinking) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = if (expanded) "收起" else "展开",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = step.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AgentStreamingBubble(
    thinking: String,
    content: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(32.dp)
                .padding(end = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (thinking.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "思考中...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Text(
                        text = thinking,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (content.isNotBlank()) {
                    Text(text = content)
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun WriteApprovalCard(
    chapterNumber: Int,
    title: String,
    content: String,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "智能体请求写入第${chapterNumber}章",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "标题：$title",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "内容预览：${content.take(200)}${if (content.length > 200) "..." else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onReject) {
                    Text("拒绝")
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onApprove) {
                    Text("批准写入")
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentInputBar(
    inputText: TextFieldValue,
    onInputChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    isLoading: Boolean,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = if (isLoading) "智能体运行中..."
                    else if (!enabled) "等待批准操作..."
                    else stringResource(R.string.chat_input_hint)
                )
            },
            singleLine = false,
            maxLines = 4,
            enabled = enabled && !isLoading
        )

        Spacer(modifier = Modifier.width(8.dp))

        if (isLoading) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "取消",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        } else {
            IconButton(
                onClick = onSend,
                enabled = inputText.text.isNotBlank() && enabled
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送",
                    tint = if (inputText.text.isNotBlank() && enabled)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun extractToolName(content: String): String? {
    // 从 "调用工具: xxx\n参数: ..." 格式中提取工具名称
    val prefix = "调用工具: "
    val startIndex = content.indexOf(prefix)
    if (startIndex == -1) return null
    val nameStart = startIndex + prefix.length
    val nameEnd = content.indexOf('\n', nameStart).let { if (it == -1) content.length else it }
    return content.substring(nameStart, nameEnd).trim()
}
