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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
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
    val outlineSummary by viewModel.outlineSummary.collectAsState()
    val isSummarizingOutline by viewModel.isSummarizingOutline.collectAsState()
    val aiModifyContent by viewModel.aiModifyContent.collectAsState()
    val isAiModifying by viewModel.isAiModifying.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var outlineExpanded by remember { mutableStateOf(false) }
    var outlineText by remember { mutableStateOf("") }
    var showAiDialog by remember { mutableStateOf(false) }
    var customRequest by remember { mutableStateOf("") }
    var showAiModifyDialog by remember { mutableStateOf(false) }
    var modifyRequest by remember { mutableStateOf("") }
    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var isTextFieldFocused by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val isKeyboardVisible = androidx.compose.foundation.layout.WindowInsets.ime
        .getBottom(density) > 0

    // 同步纲要文本
    LaunchedEffect(chapter) {
        chapter?.let { outlineText = it.outline }
    }

    // 同步 TextFieldValue 与 editContent
    LaunchedEffect(editContent) {
        if (editContent != textFieldValue.text) {
            val cursorPos = editContent.length
            textFieldValue = TextFieldValue(text = editContent, selection = androidx.compose.ui.text.TextRange(cursorPos))
        }
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
                        onClick = {
                            val selection = textFieldValue.selection
                            viewModel.setAiModifyTargetRange(
                                if (!selection.collapsed) selection.min..selection.max else null
                            )
                            showAiModifyDialog = true
                        },
                        enabled = !isAiModifying && !isAiGenerating
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.height(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.novel_ai_modify))
                    }
                    TextButton(
                        onClick = { showAiDialog = true },
                        enabled = !isAiGenerating && !isAiModifying
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
                .imePadding()
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
                Column {
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.summarizeOutline() },
                            enabled = !isSummarizingOutline && editContent.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.height(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.novel_outline_summarize))
                        }
                        if (isSummarizingOutline) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                        }
                    }
                    // 纲要总结预览
                    if (outlineSummary.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                    MaterialTheme.shapes.medium
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = outlineSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 150.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                Button(
                                    onClick = {
                                        viewModel.acceptOutlineSummary()
                                        outlineText = viewModel.chapter.value?.outline ?: outlineText
                                    },
                                    enabled = !isSummarizingOutline
                                ) {
                                    Text(stringResource(R.string.novel_ai_accept))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.discardOutlineSummary() },
                                    enabled = !isSummarizingOutline
                                ) {
                                    Text(stringResource(R.string.novel_ai_discard))
                                }
                            }
                        }
                    }
                }
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

            // AI 修改内容预览
            if (aiModifyContent.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.shapes.medium
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.novel_ai_modify),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = aiModifyContent,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Button(
                            onClick = { viewModel.acceptAiModify() },
                            enabled = !isAiModifying
                        ) {
                            Text(stringResource(R.string.novel_ai_accept))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.discardAiModify() },
                            enabled = !isAiModifying
                        ) {
                            Text(stringResource(R.string.novel_ai_discard))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // AI 生成中指示器
            if ((isAiGenerating || isAiModifying) && aiStreamingContent.isBlank() && aiModifyContent.isBlank()) {
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

            // 自定义操作栏（替代系统选中菜单）
            AnimatedVisibility(visible = isTextFieldFocused) {
                val hasSelection = !textFieldValue.selection.collapsed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasSelection) {
                        Text(
                            text = stringResource(R.string.novel_ai_modify_selected, textFieldValue.selection.length),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    // 删除选中
                    IconButton(
                        onClick = {
                            val sel = textFieldValue.selection
                            val newContent = editContent.removeRange(sel.min, sel.max)
                            viewModel.updateContent(newContent)
                            textFieldValue = TextFieldValue(
                                text = newContent,
                                selection = TextRange(sel.min)
                            )
                        },
                        enabled = hasSelection
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.novel_chapter_delete_selection),
                            tint = if (hasSelection) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.height(20.dp)
                        )
                    }
                    // 复制
                    IconButton(
                        onClick = {
                            val sel = textFieldValue.selection
                            if (!sel.collapsed) {
                                val selectedText = editContent.substring(sel.min, sel.max)
                                clipboardManager.setText(AnnotatedString(selectedText))
                            }
                        },
                        enabled = hasSelection
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.novel_chapter_copy),
                            modifier = Modifier.height(20.dp)
                        )
                    }
                    // 唤起/隐藏输入法
                    IconButton(onClick = {
                        if (isKeyboardVisible) {
                            keyboardController?.hide()
                        } else {
                            keyboardController?.show()
                        }
                    }) {
                        Icon(
                            if (isKeyboardVisible) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                            contentDescription = stringResource(R.string.novel_chapter_toggle_keyboard),
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }
            }

            // 正文编辑区
            val customSelectionColors = TextSelectionColors(
                handleColor = MaterialTheme.colorScheme.primary,
                backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            val interactionSource = remember { MutableInteractionSource() }
            val rootView = LocalView.current.rootView
            CompositionLocalProvider(
                LocalTextSelectionColors provides customSelectionColors
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        viewModel.updateContent(newValue.text)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .onGloballyPositioned {
                            // 查找底层 EditText 并禁用自动弹出键盘
                            val editText = findEditText(rootView)
                            editText?.showSoftInputOnFocus = false
                        }
                        .onFocusChanged { focusState ->
                            isTextFieldFocused = focusState.isFocused
                            if (!focusState.isFocused) {
                                keyboardController?.hide()
                            }
                        },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    interactionSource = interactionSource,
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

    // AI 修改对话框
    if (showAiModifyDialog) {
        val selection = textFieldValue.selection
        val selectedText = if (!selection.collapsed) {
            editContent.substring(selection.min, selection.max)
        } else {
            ""
        }
        AiModifyDialog(
            selectedText = selectedText,
            allContentLength = editContent.length,
            modifyRequest = modifyRequest,
            onModifyRequestChange = { modifyRequest = it },
            onDismiss = { showAiModifyDialog = false },
            onConfirm = {
                showAiModifyDialog = false
                val textToModify = if (selectedText.isNotBlank()) selectedText else editContent
                viewModel.startAiModify(textToModify, modifyRequest)
                modifyRequest = ""
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

@Composable
private fun AiModifyDialog(
    selectedText: String,
    allContentLength: Int,
    modifyRequest: String,
    onModifyRequestChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val hasSelection = selectedText.isNotBlank()

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.novel_ai_modify)) },
        text = {
            Column {
                Text(
                    text = if (hasSelection) {
                        stringResource(R.string.novel_ai_modify_selected, selectedText.length)
                    } else {
                        stringResource(R.string.novel_ai_modify_all, allContentLength)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (hasSelection) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                MaterialTheme.shapes.small
                            )
                            .padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = modifyRequest,
                    onValueChange = onModifyRequestChange,
                    label = { Text(stringResource(R.string.novel_ai_modify_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = modifyRequest.isNotBlank()
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

/**
 * 递归查找 View 层级中的 EditText
 */
private fun findEditText(view: View): EditText? {
    if (view is EditText) return view
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val result = findEditText(view.getChildAt(i))
            if (result != null) return result
        }
    }
    return null
}
