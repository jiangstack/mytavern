package org.jiangstack.mytavern.ui.interactive

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import org.jiangstack.mytavern.MyTavernApplication
import org.jiangstack.mytavern.R
import org.jiangstack.mytavern.domain.model.InteractiveGameImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveGameAlbumScreen(
    gameId: Long,
    onNavigateBack: () -> Unit
) {
    val container = (LocalContext.current.applicationContext as MyTavernApplication).container
    val viewModel: InteractiveGameAlbumViewModel = viewModel(
        key = "album_$gameId",
        factory = InteractiveGameAlbumViewModel.factory(
            gameId,
            container.interactiveGameRepository,
            container.interactiveGameImageRepository
        )
    )

    val game by viewModel.game.collectAsState()
    val images by viewModel.images.collectAsState()
    val message by viewModel.message.collectAsState()

    var previewImage by remember { mutableStateOf<InteractiveGameImage?>(null) }
    var imageToDelete by remember { mutableStateOf<InteractiveGameImage?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        game?.title ?: stringResource(R.string.image_album_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { innerPadding ->
        if (images.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.image_album_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(images, key = { it.id }) { image ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            AsyncImage(
                                model = image.localUri ?: image.remoteUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clickable { previewImage = image },
                                contentScale = ContentScale.Crop
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                            ) {
                                TextButton(
                                    onClick = { viewModel.setAsBackground(image) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.image_generate_set_background))
                                }
                                IconButton(
                                    onClick = { imageToDelete = image }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.interactive_clear),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Preview dialog
    previewImage?.let { image ->
        AlertDialog(
            onDismissRequest = { previewImage = null },
            confirmButton = {
                TextButton(onClick = { previewImage = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = {
                AsyncImage(
                    model = image.localUri ?: image.remoteUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        )
    }

    // Delete confirmation
    imageToDelete?.let { image ->
        AlertDialog(
            onDismissRequest = { imageToDelete = null },
            title = { Text(stringResource(R.string.image_api_config_delete_title)) },
            text = { Text(stringResource(R.string.image_api_config_delete_message, "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteImage(image)
                        imageToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { imageToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
