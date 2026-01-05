package com.example.nitishgallery.data

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Repository for managing device images through the MediaStore API.
 * Handles loading, deleting, and saving images to the device storage.
 */
class ImageRepository(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Loads all images from the device's external storage.
     * @param sortOption Determines the order in which images are returned.
     * @return A list of [Image] objects sorted according to the given option.
     */
    suspend fun getImages(sortOption: SortOption = SortOption.DATE): List<Image> = withContext(Dispatchers.IO) {
        val images = mutableListOf<Image>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.SIZE
        )

        val sortOrder = when (sortOption) {
            SortOption.DATE -> "${MediaStore.Images.Media.DATE_ADDED} DESC"
            SortOption.NAME -> "${MediaStore.Images.Media.DISPLAY_NAME} ASC"
            SortOption.SIZE -> "${MediaStore.Images.Media.SIZE} DESC"
        }

        contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val filePath = cursor.getString(dataColumn) ?: ""
                val size = cursor.getLong(sizeColumn)
                val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                images.add(Image(id, contentUri, name, dateAdded, filePath, size))
            }
        }
        images
    }

    /**
     * Deletes an image from the device storage.
     * On Android 11+, returns an IntentSender for the system delete confirmation dialog.
     * On older versions, attempts direct deletion.
     *
     * @param image The image to delete.
     * @return An [IntentSender] if user confirmation is required, null if deletion was successful.
     */
    suspend fun deleteImage(image: Image): IntentSender? = withContext(Dispatchers.IO) {
        // On Android 11+, always use createDeleteRequest to show system confirmation dialog
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val urisToDelete = listOf(image.uri)
            val pendingIntent = MediaStore.createDeleteRequest(contentResolver, urisToDelete)
            return@withContext pendingIntent.intentSender
        }
        
        // On Android 10 (Q), try direct delete first, then handle RecoverableSecurityException
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            try {
                val deletedRows = contentResolver.delete(image.uri, null, null)
                if (deletedRows > 0) return@withContext null
            } catch (e: SecurityException) {
                val recoverableException = e as? RecoverableSecurityException
                if (recoverableException != null) {
                    return@withContext recoverableException.userAction.actionIntent.intentSender
                }
                throw e
            }
        }
        
        // On Android 9 and below, direct delete should work
        try {
            val deletedRows = contentResolver.delete(image.uri, null, null)
            if (deletedRows > 0) return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        null
    }

    /**
     * Saves (copies) an image to the "Scroll and Delete" folder in Pictures.
     *
     * @param image The image to save.
     * @return True if the save was successful, false otherwise.
     */
    suspend fun saveImage(image: Image): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = contentResolver.openInputStream(image.uri) ?: return@withContext false
            val bytes = inputStream.readBytes()
            inputStream.close()

            val folderName = "Scroll and Delete"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, image.name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/*")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$folderName")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val imageUri = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return@withContext false

                contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                    outputStream.write(bytes)
                }

                // Mark as not pending
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(imageUri, contentValues, null, null)
                
                true
            } else {
                // For older Android versions
                @Suppress("DEPRECATION")
                val rootDir = Environment.getExternalStorageDirectory()
                val targetDir = File(rootDir, folderName)
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                
                val destFile = File(targetDir, image.name)
                FileOutputStream(destFile).use { outputStream ->
                    outputStream.write(bytes)
                }
                
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

