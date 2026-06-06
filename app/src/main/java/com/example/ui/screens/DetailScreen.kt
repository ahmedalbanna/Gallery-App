package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.GalleryMedia
import com.example.ui.components.ZoomableImage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    initialIndex: Int,
    imagesList: List<GalleryMedia>,
    onBackClick: () -> Unit,
    onToggleFavorite: (GalleryMedia) -> Unit,
    onDeleteMedia: (GalleryMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Safety check for empty lists
    if (imagesList.isEmpty()) {
        LaunchedEffect(Unit) { onBackClick() }
        return
    }

    // Horizontal pager to slide through images
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, imagesList.size - 1)) {
        imagesList.size
    }

    val currentMedia = imagesList.getOrNull(pagerState.currentPage) ?: return

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Black, // Dark cinematic ambient theme
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentMedia.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} / ${imagesList.size}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.dialog_cancel),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            // Elegant translucent overlay Bottom Action Bar
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorite Toggle Button
                    IconButton(
                        onClick = { onToggleFavorite(currentMedia) },
                        modifier = Modifier.testTag("action_favorite")
                    ) {
                        Icon(
                            imageVector = if (currentMedia.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(R.string.btn_favorite),
                            tint = if (currentMedia.isFavorite) MaterialTheme.colorScheme.error else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Share Button
                    IconButton(
                        onClick = { shareImage(context, currentMedia) },
                        modifier = Modifier.testTag("action_share")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.btn_share),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Info Details Button
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.testTag("action_info")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.btn_details),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.testTag("action_delete")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.btn_delete),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(innerPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 16.dp
            ) { page ->
                val media = imagesList.getOrNull(page)
                if (media != null) {
                    ZoomableImage(
                        model = media.uri,
                        contentDescription = media.displayName,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // 🗑️ Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.dialog_delete_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = stringResource(R.string.dialog_delete_desc))
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        showDeleteDialog = false
                        onDeleteMedia(currentMedia)
                    }
                ) {
                    Text(text = stringResource(R.string.btn_delete), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    // ℹ️ Photo Details Dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.info_title),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Divider()
                    DetailRow(label = stringResource(R.string.info_name), value = currentMedia.displayName)
                    DetailRow(label = stringResource(R.string.info_size), value = formatSize(currentMedia.size))
                    DetailRow(label = stringResource(R.string.info_date), value = formatDate(currentMedia.dateAdded))
                    DetailRow(label = stringResource(R.string.info_path), value = currentMedia.path)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(text = stringResource(R.string.info_close))
                }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

// Format media file size in bytes into KB, MB, etc.
private fun formatSize(sizeInBytes: Long): String {
    if (sizeInBytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(sizeInBytes.toDouble()) / Math.log10(1024.0)).toInt()
    return try {
        String.format(
            Locale.getDefault(),
            "%.2f %s",
            sizeInBytes / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups]
        )
    } catch (e: Exception) {
        "$sizeInBytes B"
    }
}

// Convert date seconds count to human-readable date format
private fun formatDate(dateInSeconds: Long): String {
    return try {
        val date = Date(dateInSeconds * 1000)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sdf.format(date)
    } catch (e: Exception) {
        dateInSeconds.toString()
    }
}

// Trigger native android chooser stream to share image files
private fun shareImage(context: Context, media: GalleryMedia) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, media.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.btn_share)))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, context.getString(R.string.toast_share_failed), Toast.LENGTH_SHORT).show()
    }
}
