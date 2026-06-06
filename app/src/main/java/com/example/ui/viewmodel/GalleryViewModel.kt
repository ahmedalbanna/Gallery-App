package com.example.ui.viewmodel

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.GalleryMedia
import com.example.data.repository.GalleryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface GalleryUiState {
    object Loading : GalleryUiState
    data class Success(
        val allImages: List<GalleryMedia>,
        val albums: Map<String, List<GalleryMedia>>,
        val favorites: List<GalleryMedia>
    ) : GalleryUiState
    data class Error(val message: String) : GalleryUiState
}

class GalleryViewModel(private val repository: GalleryRepository) : ViewModel() {

    private val _deviceImages = MutableStateFlow<List<GalleryMedia>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    // Combine loaded device images with latest favorite status from Room Database
    val uiState: StateFlow<GalleryUiState> = combine(
        _deviceImages,
        repository.favoriteIdsStateFlow,
        _isLoading
    ) { deviceImages, favoriteIds, isLoading ->
        if (isLoading && deviceImages.isEmpty()) {
            GalleryUiState.Loading
        } else {
            val favoriteIdsSet = favoriteIds.toSet()
            val enrichedImages = deviceImages.map { media ->
                media.copy(isFavorite = favoriteIdsSet.contains(media.id))
            }
            val albums = enrichedImages.groupBy { it.albumName }
            val favorites = enrichedImages.filter { it.isFavorite }

            GalleryUiState.Success(
                allImages = enrichedImages,
                albums = albums,
                favorites = favorites
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GalleryUiState.Loading
    )

    // Handle intent sender request when Android Q+ requests user confirmation for file changes/deletions
    var pendingDeleteMedia: GalleryMedia? = null
    private val _deleteIntentSender = MutableStateFlow<IntentSender?>(null)
    val deleteIntentSender: StateFlow<IntentSender?> = _deleteIntentSender.asStateFlow()

    fun loadImages(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = repository.fetchLocalImages(context)
                _deviceImages.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFavorite(media: GalleryMedia) {
        viewModelScope.launch {
            repository.toggleFavorite(media.id, media.uriString)
        }
    }

    fun deleteImage(context: Context, media: GalleryMedia) {
        viewModelScope.launch {
            try {
                val success = repository.deleteLocalImage(context, media)
                if (success) {
                    _deviceImages.value = _deviceImages.value.filter { it.id != media.id }
                }
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableException = e as? RecoverableSecurityException
                    if (recoverableException != null) {
                        pendingDeleteMedia = media
                        _deleteIntentSender.value = recoverableException.userAction.actionIntent.intentSender
                    } else {
                        // Soft-delete from UI if we cannot physically delete
                        _deviceImages.value = _deviceImages.value.filter { it.id != media.id }
                    }
                } else {
                    _deviceImages.value = _deviceImages.value.filter { it.id != media.id }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onIntentSenderHandled() {
        _deleteIntentSender.value = null
    }

    fun confirmPendingDeletion(mediaId: Long) {
        _deviceImages.value = _deviceImages.value.filter { it.id != mediaId }
        pendingDeleteMedia = null
    }

    // Factory to instantiate ViewModel with constructor injection
    class Factory(private val repository: GalleryRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GalleryViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return GalleryViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
