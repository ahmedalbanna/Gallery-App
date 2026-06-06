package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.data.local.FavoriteDao
import com.example.data.local.FavoriteMediaEntity
import com.example.data.model.GalleryMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GalleryRepository(private val favoriteDao: FavoriteDao) {

    // Retrieve system favorite ids reactively
    val favoriteIdsStateFlow: Flow<List<Long>> = favoriteDao.getAllFavoriteIdsFlow()

    suspend fun fetchLocalImages(context: Context): List<GalleryMedia> {
        return withContext(Dispatchers.IO) {
            val imagesList = mutableListOf<GalleryMedia>()
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE
            )

            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            try {
                // Fetch the list of already favorited IDs from Room Database
                val favoriteIds = favoriteDao.getAllFavoriteIds().toSet()

                context.contentResolver.query(
                    queryUri,
                    projection,
                    null,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                    val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val displayName = cursor.getString(displayNameColumn) ?: ""
                        val path = cursor.getString(dataColumn) ?: ""
                        val albumName = cursor.getString(bucketColumn) ?: "Others"
                        val dateAdded = cursor.getLong(dateAddedColumn)
                        val size = cursor.getLong(sizeColumn)
                        val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                        imagesList.add(
                            GalleryMedia(
                                id = id,
                                displayName = displayName,
                                uriString = contentUri.toString(),
                                path = path,
                                albumName = albumName,
                                dateAdded = dateAdded,
                                size = size,
                                isFavorite = favoriteIds.contains(id)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            imagesList
        }
    }

    suspend fun toggleFavorite(mediaId: Long, uriString: String): Boolean {
        return withContext(Dispatchers.IO) {
            val alreadyFavorite = favoriteDao.isFavorite(mediaId)
            if (alreadyFavorite) {
                favoriteDao.deleteFavoriteById(mediaId)
                false // removed
            } else {
                favoriteDao.insertFavorite(FavoriteMediaEntity(mediaId, uriString))
                true // added
            }
        }
    }

    suspend fun deleteLocalImage(context: Context, image: GalleryMedia): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Delete from local DB first
                favoriteDao.deleteFavoriteById(image.id)
                // Delete from device storage
                val contentResolver = context.contentResolver
                val deletedRows = contentResolver.delete(image.uri, null, null)
                deletedRows > 0
            } catch (e: SecurityException) {
                // Modern Android will throw SecurityException (or RecoverableSecurityException) 
                // when trying to write/delete shared media. Let it bubble up.
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
