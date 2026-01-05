package com.example.nitishgallery.data

import android.net.Uri

/**
 * Represents an image from the device's media storage.
 *
 * @property id Unique identifier from MediaStore.
 * @property uri Content URI for accessing the image.
 * @property name Display name of the image file.
 * @property dateAdded Unix timestamp when the image was added.
 * @property filePath Absolute file path (may be empty on newer Android versions).
 * @property size File size in bytes.
 */
data class Image(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateAdded: Long,
    val filePath: String = "",
    val size: Long = 0
)

