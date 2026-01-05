package com.example.nitishgallery.ui

import android.app.Application
import android.app.RecoverableSecurityException
import android.content.IntentSender
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nitishgallery.data.Image
import com.example.nitishgallery.data.ImageRepository
import com.example.nitishgallery.data.SortOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Gallery that manages image loading, sorting, and deletion.
 * Acts as an intermediary between the UI and the [ImageRepository].
 */
class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ImageRepository(application)

    private val _images = MutableStateFlow<List<Image>>(emptyList())
    /** Observable list of images currently loaded from storage. */
    val images: StateFlow<List<Image>> = _images.asStateFlow()

    private val _permissionGranted = MutableStateFlow(false)
    /** Indicates whether the required storage permissions have been granted. */
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.DATE)
    /** The current sort option for the image list. */
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    /** Indicates whether images are currently being loaded. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Updates the permission status and loads images if granted.
     * @param granted Whether storage permission was granted.
     */
    fun updatePermissionStatus(granted: Boolean) {
        _permissionGranted.value = granted
        if (granted) {
            loadImages()
        }
    }

    /**
     * Updates the sort option and reloads images.
     * @param option The new [SortOption] to apply.
     */
    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
        loadImages()
    }

    /** Loads images from the repository using the current sort option. */
    fun loadImages() {
        viewModelScope.launch {
            _isLoading.value = true
            _images.value = repository.getImages(_sortOption.value)
            _isLoading.value = false
        }
    }

    /**
     * Deletes an image from storage.
     * @param image The image to delete.
     * @param onSuccess Callback invoked on successful deletion.
     * @param onSecurityException Callback invoked when user confirmation is required (Android 10+).
     */
    fun deleteImage(image: Image, onSuccess: () -> Unit, onSecurityException: (IntentSender) -> Unit) {
        viewModelScope.launch {
            try {
                val intentSender = repository.deleteImage(image)
                
                if (intentSender != null) {
                    onSecurityException(intentSender)
                } else {
                    _images.value = _images.value.filter { it.id != image.id }
                    onSuccess()
                }
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableSecurityException = e as? RecoverableSecurityException
                    recoverableSecurityException?.let {
                        onSecurityException(it.userAction.actionIntent.intentSender)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Saves an image to the "Scroll and Delete" folder.
     * @param image The image to save.
     * @param onSuccess Callback invoked on successful save.
     * @param onError Callback invoked if the save fails.
     */
    fun saveImage(image: Image, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val success = repository.saveImage(image)
            if (success) {
                onSuccess()
            } else {
                onError()
            }
        }
    }

    /**
     * Removes an image from the in-memory list without deleting from storage.
     * Used after successful deletion confirmation from the system dialog.
     */
    fun removeImageFromList(image: Image) {
        _images.value = _images.value.filter { it.id != image.id }
    }
}

