package org.jiangstack.mytavern.ui.worldbook

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jiangstack.mytavern.MyTavernApplication
import org.jiangstack.mytavern.R
import org.jiangstack.mytavern.domain.model.WorldBookRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldBookDetailScreen(
    worldBookId: Long,
    onNavigateBack: () -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: WorldBookDetailViewModel = viewModel(
        factory = WorldBookDetailViewModel.factory(
            container.worldBookRepository,
            worldBookId
        )
    )

    val worldBook by viewModel.worldBook.collectAsState()
    val rules by viewModel.rules.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<WorldBookRule?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var ruleToDelete by remember { mutableStateOf<WorldBookRule?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(worldBook?.name ?: stringResource(R.string.worldbook_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingRule = null
                showEditDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = worldBook?.name ?: "",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = worldBook?.description ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.worldbook_rules_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(rules, key = { it.id }) { rule ->
                RuleItem(
                    rule = rule,
                    onClick = {
                        editingRule = rule
                        showEditDialog = true
                    },
                    onLongClick = {
                        ruleToDelete = rule
                        showDeleteDialog = true
                    }
                )
            }
        }
    }

    if (showEditDialog) {
        RuleEditDialog(
            rule = editingRule,
            worldBookId = worldBookId,
            onDismiss = { showEditDialog = false },
            onConfirm = { rule ->
                viewModel.saveRule(rule)
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog && ruleToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.worldbook_rule_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.worldbook_rule_delete_message,
                        ruleToDelete!!.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRule(ruleToDelete!!)
                        showDeleteDialog = false
                        ruleToDelete = null
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
private fun RuleItem(
    rule: WorldBookRule,
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
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = rule.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = rule.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${stringResource(R.string.worldbook_rule_match_type)}: ${rule.matchType}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun RuleEditDialog(
    rule: WorldBookRule?,
    worldBookId: Long,
    onDismiss: () -> Unit,
    onConfirm: (WorldBookRule) -> Unit
) {
    var name by remember { mutableStateOf(rule?.name ?: "") }
    var description by remember { mutableStateOf(rule?.description ?: "") }
    var matchType by remember { mutableStateOf(rule?.matchType ?: "contains") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (rule == null)
                    stringResource(R.string.worldbook_rule_create_title)
                else
                    stringResource(R.string.worldbook_rule_edit_title)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.worldbook_rule_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.worldbook_rule_description)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = matchType,
                    onValueChange = { matchType = it },
                    label = { Text(stringResource(R.string.worldbook_rule_match_type)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        WorldBookRule(
                            id = rule?.id ?: 0,
                            worldBookId = worldBookId,
                            name = name,
                            description = description,
                            matchType = matchType
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
