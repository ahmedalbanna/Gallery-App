package com.example.data.model

import android.net.Uri

data class GalleryMedia(
    val id: Long,
    val displayName: String,
    val uriString: String,
    val path: String,
    val albumName: String,
    val dateAdded: Long,
    val size: Long,
    val isFavorite: Boolean = false
) {
    val uri: Uri get() = Uri.parse(uriString)
}
