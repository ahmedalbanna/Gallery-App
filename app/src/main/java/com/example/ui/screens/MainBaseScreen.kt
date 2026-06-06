package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.GalleryMedia
import com.example.ui.components.AlbumGridItem
import com.example.ui.components.MediaGridItem
import com.example.ui.viewmodel.GalleryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainBaseScreen(
    uiState: GalleryUiState,
    onMediaClick: (GalleryMedia, List<GalleryMedia>) -> Unit,
    onAlbumClick: (String, List<GalleryMedia>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_images),
        stringResource(R.string.tab_albums),
        stringResource(R.string.tab_favorites)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                },
                actions = {
                    IconButton(onClick = { /* Simulated Search action */ }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    // Personalized AVATAR Badge: "AH" for User email (ahmedalbannacode@gmail.com)
                    Surface(
                        modifier = Modifier
                            .padding(end = 12.dp, start = 4.dp)
                            .size(38.dp),
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "AH",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Elegant M3 Pill-shaped Tab Row from the "Sleek Interface" template
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    val borderColor = if (isSelected) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = containerColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .clickable { selectedTab = index }
                            .testTag("tab_pill_$index")
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor
                            ),
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body content based on selected tab
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (uiState) {
                    is GalleryUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is GalleryUiState.Success -> {
                        when (selectedTab) {
                            0 -> PhotosTabContent(
                                imagesList = uiState.allImages,
                                onMediaClick = onMediaClick
                            )
                            1 -> AlbumsTabContent(
                                albumsMap = uiState.albums,
                                onAlbumClick = onAlbumClick
                            )
                            2 -> FavoritesTabContent(
                                favoritesList = uiState.favorites,
                                onMediaClick = onMediaClick
                            )
                        }
                    }
                    is GalleryUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.message,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotosTabContent(
    imagesList: List<GalleryMedia>,
    onMediaClick: (GalleryMedia, List<GalleryMedia>) -> Unit
) {
    if (imagesList.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Image,
            message = stringResource(R.string.no_images_found)
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(imagesList, key = { it.id }) { media ->
                MediaGridItem(
                    media = media,
                    onClick = { onMediaClick(media, imagesList) }
                )
            }
        }
    }
}

@Composable
fun AlbumsTabContent(
    albumsMap: Map<String, List<GalleryMedia>>,
    onAlbumClick: (String, List<GalleryMedia>) -> Unit
) {
    if (albumsMap.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.FolderOpen,
            message = stringResource(R.string.no_albums_found)
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(albumsMap.keys.toList(), key = { it }) { albumName ->
                val mediaList = albumsMap[albumName] ?: emptyList()
                AlbumGridItem(
                    albumName = albumName,
                    mediaList = mediaList,
                    onClick = { onAlbumClick(albumName, mediaList) }
                )
            }
        }
    }
}

@Composable
fun FavoritesTabContent(
    favoritesList: List<GalleryMedia>,
    onMediaClick: (GalleryMedia, List<GalleryMedia>) -> Unit
) {
    if (favoritesList.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.FavoriteBorder,
            message = stringResource(R.string.no_favorites_found)
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(favoritesList, key = { it.id }) { media ->
                MediaGridItem(
                    media = media,
                    onClick = { onMediaClick(media, favoritesList) }
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            ),
            textAlign = TextAlign.Center
        )
    }
}
