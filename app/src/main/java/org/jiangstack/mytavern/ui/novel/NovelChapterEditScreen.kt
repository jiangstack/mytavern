package org.jiangstack.mytavern.ui.novel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jiangstack.mytavern.MyTavernApplication
import org.jiangstack.mytavern.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelChapterEditScreen(
    novelId: Long,
    chapterId: Long,
    onNavigateBack: () -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: NovelChapterEditViewModel = viewModel(
        factory = NovelChapterEditViewModel.factory(
            container.novelRepository,
            container.worldBookRepository,
            container.characterRepository,
            container.userPreferencesRepository,
            container.llmService,
            novelId,
            chapterId
        )
    )

    val chapter by viewModel.chapter.collectAsState()
    val editContent by viewModel.editContent.collectAsState()
    val aiStreamingContent by viewModel.aiStreamingContent.collectAsState()
    val isAiGenerating by viewModel.isAiGenerating.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var outlineExpanded by remember { mutableStateOf(false) }
    var outlineText by remember { mutableStateOf("") }
    var showAiDialog by remember { mutableStateOf(false) }
    var customRequest by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    // 同步纲要文本
    LaunchedEffect(chapter) {
        chapter?.let { outlineText = it.outline }
    }

    // 错误提示
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
                        text = chapter?.let { "第${it.chapterNumber}章 ${it.title}" } ?: "",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveChapter()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showAiDialog = true },
                        enabled = !isAiGenerating
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.height(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.novel_ai_continue))
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
        ) {
            // 纲要折叠区
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.novel_chapter_outline),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { outlineExpanded = !outlineExpanded }) {
                    Icon(
                        if (outlineExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            }

            AnimatedVisibility(visible = outlineExpanded) {
                OutlinedTextField(
                    value = outlineText,
                    onValueChange = {
                        outlineText = it
                        viewModel.updateOutline(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    minLines = 2,
                    maxLines = 6,
                    placeholder = { Text(stringResource(R.string.novel_chapter_outline)) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // AI 续写内容预览
            if (aiStreamingContent.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.shapes.medium
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.novel_ai_generating),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = aiStreamingContent,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Button(
                            onClick = { viewModel.acceptAiContent() },
                            enabled = !isAiGenerating
                        ) {
                            Text(stringResource(R.string.novel_ai_accept))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.discardAiContent() },
                            enabled = !isAiGenerating
                        ) {
                            Text(stringResource(R.string.novel_ai_discard))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // AI 生成中指示器
            if (isAiGenerating && aiStreamingContent.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.height(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.novel_ai_generating),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // 正文编辑区
            BasicTextField(
                value = editContent,
                onValueChange = { viewModel.updateContent(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 26.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (editContent.isEmpty()) {
                            Text(
                                text = stringResource(R.string.novel_chapter_content),
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }

    // AI 续写对话框
    if (showAiDialog) {
        AiContinueDialog(
            customRequest = customRequest,
            onCustomRequestChange = { customRequest = it },
            onDismiss = { showAiDialog = false },
            onConfirm = {
                showAiDialog = false
                viewModel.startAiContinue(customRequest)
                customRequest = ""
            }
        )
    }
}

@Composable
private fun AiContinueDialog(
    customRequest: String,
    onCustomRequestChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.novel_ai_continue)) },
        text = {
            Column {
                Text(
                    text = "AI 将读取小说信息、世界书、角色和历史章节作为上下文进行续写。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = customRequest,
                    onValueChange = onCustomRequestChange,
                    label = { Text(stringResource(R.string.novel_ai_continue_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
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
