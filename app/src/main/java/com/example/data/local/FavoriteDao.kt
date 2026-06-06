package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_media ORDER BY dateAddedToFavorites DESC")
    fun getAllFavoritesFlow(): Flow<List<FavoriteMediaEntity>>

    @Query("SELECT mediaId FROM favorite_media")
    suspend fun getAllFavoriteIds(): List<Long>

    @Query("SELECT mediaId FROM favorite_media")
    fun getAllFavoriteIdsFlow(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteMediaEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteMediaEntity)

    @Query("DELETE FROM favorite_media WHERE mediaId = :mediaId")
    suspend fun deleteFavoriteById(mediaId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_media WHERE mediaId = :mediaId LIMIT 1)")
    suspend fun isFavorite(mediaId: Long): Boolean
}
