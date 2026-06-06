package com.example

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppDatabase
import com.example.data.model.GalleryMedia
import com.example.data.repository.GalleryRepository
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.MainBaseScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GalleryViewModel

sealed interface Screen {
    object Base : Screen
    data class AlbumDetail(val name: String, val list: List<GalleryMedia>) : Screen
    data class Detail(val initialIndex: Int, val list: List<GalleryMedia>) : Screen
}

class MainActivity : ComponentActivity() {

    // Instantiate Room Database and Repository
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy { GalleryRepository(database.favoriteDao()) }

    // Instantiate dynamic ViewModel using standard Factory injection
    private val viewModel: GalleryViewModel by viewModels {
        GalleryViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        GalleryAppContainer(viewModel = viewModel)
                    }
                }
            }
        }
    }

    @Composable
    fun GalleryAppContainer(viewModel: GalleryViewModel) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Base) }
        var hasPermission by remember { mutableStateOf(checkStoragePermission()) }

        // StartIntentSenderForResult launcher for Scoped Storage deletions
        val intentSenderLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.pendingDeleteMedia?.let { media ->
                    viewModel.confirmPendingDeletion(media.id)
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.toast_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                viewModel.pendingDeleteMedia = null
            }
            viewModel.onIntentSenderHandled()
        }

        // Native Permission launcher
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasPermission = isGranted
            if (isGranted) {
                viewModel.loadImages(applicationContext)
            } else {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.permission_desc),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Handle Recoverable Delete Intent Senders (Android 10+)
        val deleteSender by viewModel.deleteIntentSender.collectAsStateWithLifecycle()
        LaunchedEffect(deleteSender) {
            deleteSender?.let { sender ->
                intentSenderLauncher.launch(
                    IntentSenderRequest.Builder(sender).build()
                )
            }
        }

        // Automatically trigger image loading if permission is already granted
        LaunchedEffect(hasPermission) {
            if (hasPermission) {
                viewModel.loadImages(applicationContext)
            }
        }

        // Coordinate Native System Back Presses
        when (val screen = currentScreen) {
            is Screen.Base -> {
                // Let the system handle default back exit
            }
            is Screen.AlbumDetail -> {
                BackHandler {
                    currentScreen = Screen.Base
                }
            }
            is Screen.Detail -> {
                BackHandler {
                    // Navigate back to where we came from (either base or album detail)
                    currentScreen = Screen.Base
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (!hasPermission) {
                PermissionRequestScreen {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_IMAGES
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    permissionLauncher.launch(permission)
                }
            } else {
                when (val screen = currentScreen) {
                    Screen.Base -> {
                        MainBaseScreen(
                            uiState = uiState,
                            onMediaClick = { media, list ->
                                currentScreen = Screen.Detail(list.indexOf(media), list)
                            },
                            onAlbumClick = { albumName, list ->
                                currentScreen = Screen.AlbumDetail(albumName, list)
                            }
                        )
                    }
                    is Screen.AlbumDetail -> {
                        AlbumDetailScreen(
                            albumName = screen.name,
                            mediaList = screen.list,
                            onBackClick = { currentScreen = Screen.Base },
                            onMediaClick = { media, list ->
                                currentScreen = Screen.Detail(list.indexOf(media), list)
                            }
                        )
                    }
                    is Screen.Detail -> {
                        DetailScreen(
                            initialIndex = screen.initialIndex,
                            imagesList = screen.list,
                            onBackClick = {
                                currentScreen = Screen.Base
                            },
                            onToggleFavorite = { media ->
                                viewModel.toggleFavorite(media)
                                val msgId = if (media.isFavorite) R.string.toast_removed_favorite else R.string.toast_added_favorite
                                Toast.makeText(this@MainActivity, getString(msgId), Toast.LENGTH_SHORT).show()
                            },
                            onDeleteMedia = { media ->
                                viewModel.deleteImage(applicationContext, media)
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun PermissionRequestScreen(onGrantClick: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.permission_desc),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(text = stringResource(R.string.permission_grant_button))
            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}
