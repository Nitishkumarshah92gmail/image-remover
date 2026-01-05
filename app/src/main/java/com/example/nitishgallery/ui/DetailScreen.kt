package com.example.nitishgallery.ui

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-screen detail view for browsing images with a horizontal pager.
 * Supports swipe gestures, sharing, deletion, and viewing image info.
 *
 * @param viewModel The shared [GalleryViewModel] instance.
 * @param initialIndex The starting image index in the pager.
 * @param onBack Callback invoked when navigating back to the gallery.
 */

@Composable
fun DetailScreen(
    viewModel: GalleryViewModel,
    initialIndex: Int,
    onBack: () -> Unit
) {
    val images by viewModel.images.collectAsState()
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { images.size })
    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var imageToDeleteIndex by remember { mutableStateOf(-1) }

    // Intent Sender for Android 10+ deletion
    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // User approved, retry the delete call
            val image = images.getOrNull(imageToDeleteIndex)
            if (image != null) {
                viewModel.deleteImage(
                    image = image,
                    onSuccess = {
                        Toast.makeText(context, "Image deleted", Toast.LENGTH_SHORT).show()
                    },
                    onSecurityException = { /* Should not happen again immediately */ }
                )
            }
        }
    }

    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (images.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val image = images.getOrNull(page)
                if (image != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = { },
                                    onDragCancel = { },
                                    onVerticalDrag = { change, dragAmount ->
                                        val y = dragAmount
                                        if (y < -50) { // Swipe Up -> Delete
                                            imageToDeleteIndex = page
                                            showDeleteDialog = true
                                            change.consume()
                                        } else if (y > 50) { // Swipe Down -> Save
                                            Toast.makeText(context, "Added to Favorites", Toast.LENGTH_SHORT).show()
                                            change.consume()
                                        }
                                    }
                                )
                            }
                    ) {
                        AsyncImage(
                            model = image.uri,
                            contentDescription = image.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Top Bar with Back, Share, and Delete Buttons
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Row {
                    // Info Button
                    IconButton(
                        onClick = {
                            showInfoDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Share Button
                    IconButton(
                        onClick = {
                            val image = images.getOrNull(pagerState.currentPage)
                            if (image != null) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/*"
                                    putExtra(Intent.EXTRA_STREAM, image.uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = {
                            imageToDeleteIndex = pagerState.currentPage
                            showDeleteDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Position Indicator
            Text(
                text = "${pagerState.currentPage + 1} of ${images.size}",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )

        } else {
            Text(
                text = "No images", 
                color = MaterialTheme.colorScheme.primary, 
                modifier = Modifier.align(Alignment.Center)
            )
            // Back button even if no images (edge case)
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Image?") },
                text = { Text("Are you sure you want to delete this image?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            val image = images.getOrNull(imageToDeleteIndex)
                            if (image != null) {
                                viewModel.deleteImage(
                                    image = image,
                                    onSuccess = {
                                        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                                    },
                                    onSecurityException = { intentSender ->
                                        intentSenderLauncher.launch(
                                            IntentSenderRequest.Builder(intentSender).build()
                                        )
                                    }
                                )
                            }
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showInfoDialog) {
            val image = images.getOrNull(pagerState.currentPage)
            if (image != null) {
                AlertDialog(
                    onDismissRequest = { showInfoDialog = false },
                    title = { Text("Image Details") },
                    text = {
                        Column {
                            Text("Name: ${image.name}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Date: ${formatDate(image.dateAdded)}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Size: ${formatSize(image.size)}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Path: ${image.filePath}")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showInfoDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp * 1000))
}

private fun formatSize(size: Long): String {
    val kb = size / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1 -> String.format("%.2f MB", mb)
        kb >= 1 -> String.format("%.2f KB", kb)
        else -> "$size Bytes"
    }
}
