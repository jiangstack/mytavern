package org.jiangstack.mytavern.ui.interactive

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.jiangstack.mytavern.R
import org.jiangstack.mytavern.ui.interactive.InteractiveGamePlayViewModel.ImageGenState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGenerateDialog(
    initialPrompt: String,
    initialParamsJson: String,
    imageGenState: ImageGenState,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onGenerate: (prompt: String, paramsJson: String) -> Unit,
    onSave: (url: String) -> Unit,
    onSetBackground: (url: String) -> Unit,
    onRetry: (prompt: String, paramsJson: String) -> Unit
) {
    var prompt by remember { mutableStateOf(initialPrompt) }
    var paramsJson by remember { mutableStateOf(initialParamsJson) }
    var paramsError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        prompt = initialPrompt
        paramsJson = initialParamsJson
    }

    val isParamsValid = remember(paramsJson) {
        if (paramsJson.isBlank()) {
            paramsError = false
            true
        } else {
            val valid = try {
                kotlinx.serialization.json.Json.Default.parseToJsonElement(paramsJson)
                paramsJson.trim().startsWith("{")
            } catch (_: Exception) {
                false
            }
            paramsError = !valid
            valid
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.image_generate_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text(stringResource(R.string.image_generate_prompt)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = paramsJson,
                    onValueChange = { paramsJson = it },
                    label = { Text(stringResource(R.string.image_generate_params)) },
                    isError = paramsError,
                    supportingText = {
                        if (paramsError) {
                            Text(stringResource(R.string.image_generate_params_invalid))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(16.dp))

                when (imageGenState) {
                    is ImageGenState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stringResource(R.string.image_generate_generating))
                                if (imageGenState.attempt > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "第 ${imageGenState.attempt}/${imageGenState.maxAttempts} 次轮询",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = onCancel) {
                                    Text(stringResource(R.string.cancel))
                                }
                            }
                        }
                    }
                    is ImageGenState.Success -> {
                        if (imageGenState.urls.isEmpty()) {
                            Text(
                                text = "未返回图片",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentPadding = PaddingValues(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(imageGenState.urls, key = { it }) { url ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column {
                                            AsyncImage(
                                                model = url,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(120.dp),
                                                contentScale = ContentScale.Crop
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                TextButton(
                                                    onClick = { onSave(url) },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(stringResource(R.string.image_generate_save))
                                                }
                                                TextButton(
                                                    onClick = { onSetBackground(url) },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(stringResource(R.string.image_generate_set_background))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is ImageGenState.Error -> {
                        Column {
                            Text(
                                text = imageGenState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onRetry(prompt, paramsJson) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.image_generate_retry))
                            }
                        }
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGenerate(prompt, paramsJson) },
                enabled = prompt.isNotBlank() && isParamsValid && imageGenState !is ImageGenState.Loading
            ) {
                Text(stringResource(R.string.interactive_generate_image))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
