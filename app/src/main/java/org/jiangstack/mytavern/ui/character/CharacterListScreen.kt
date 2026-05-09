package org.jiangstack.mytavern.ui.character

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jiangstack.mytavern.MyTavernApplication
import org.jiangstack.mytavern.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    onNavigateToDetail: (Long) -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: CharacterListViewModel = viewModel(
        factory = CharacterListViewModel.factory(container.characterRepository)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.character_list_title)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: add character */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add Character")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Character List (TODO)",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
