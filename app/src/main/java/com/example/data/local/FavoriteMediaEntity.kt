package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_media")
data class FavoriteMediaEntity(
    @PrimaryKey
    val mediaId: Long,
    val uriString: String,
    val dateAddedToFavorites: Long = System.currentTimeMillis()
)
