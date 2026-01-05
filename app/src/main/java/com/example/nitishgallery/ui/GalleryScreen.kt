package com.example.nitishgallery.ui

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nitishgallery.data.Image
import com.example.nitishgallery.data.SortOption
import kotlinx.coroutines.launch



import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = viewModel(),
    listState: LazyListState = rememberLazyListState(),
    onImageClick: (Int) -> Unit
) {
    val images by viewModel.images.collectAsState()
    val permissionGranted by viewModel.permissionGranted.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var pendingDeleteImage by remember { mutableStateOf<Image?>(null) }
    var pendingAction by remember { mutableStateOf("DELETE") } // DELETE or MOVE
    var resetCardKey by remember { mutableStateOf(0) }
    var resetTargetId by remember { mutableStateOf<Long?>(null) }

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { perms ->
            val granted = perms.values.all { it }
            viewModel.updatePermissionStatus(granted)
        }
    )
    
    // Intent Sender for Android 10+ deletion - handles system delete permission dialog
    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingDeleteImage?.let { image ->
                viewModel.removeImageFromList(image)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = if (pendingAction == "MOVE") 
                            "\"${image.name}\" moved to Scroll and Delete folder" 
                        else 
                            "Image deleted permanently",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        } else {
            // Delete was cancelled - reset the card position
            resetTargetId = pendingDeleteImage?.id
            resetCardKey++
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Delete cancelled",
                    duration = SnackbarDuration.Short
                )
            }
        }
        pendingDeleteImage = null
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissions.toTypedArray())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Scroll & Delete",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    val currentSortOption by viewModel.sortOption.collectAsState()

                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Date (Newest)", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                viewModel.updateSortOption(SortOption.DATE)
                                expanded = false
                            },
                            trailingIcon = {
                                if (currentSortOption == SortOption.DATE) {
                                    Text("✓", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Name (A-Z)", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                viewModel.updateSortOption(SortOption.NAME)
                                expanded = false
                            },
                            trailingIcon = {
                                if (currentSortOption == SortOption.NAME) {
                                    Text("✓", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Size (Largest)", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                viewModel.updateSortOption(SortOption.SIZE)
                                expanded = false
                            },
                            trailingIcon = {
                                if (currentSortOption == SortOption.SIZE) {
                                    Text("✓", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    }
                }
            )
        },


        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    ) { padding ->
        if (permissionGranted) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Made by Nitish❤️",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                images.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No images found",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Your device gallery is empty",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        // Instructions header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 12.dp)
                        ) {
                            Text(
                                text = "← Swipe LEFT to DELETE  |  Swipe RIGHT to SAVE →",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 0.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = images,
                                key = { it.id }
                            ) { image ->
                                val index = images.indexOf(image)
                                SwipeableImageCard(
                                    image = image,
                                    resetKey = if (resetTargetId == image.id) resetCardKey else 0,
                                    onClick = { onImageClick(index) },

                                    onSwipeLeft = {
                                        // Delete action
                                        pendingDeleteImage = image
                                        pendingAction = "DELETE"
                                        viewModel.deleteImage(
                                            image = image,
                                            onSuccess = {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "\"${image.name}\" deleted",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            },
                                            onSecurityException = { intentSender ->
                                                intentSenderLauncher.launch(
                                                    IntentSenderRequest.Builder(intentSender).build()
                                                )
                                            }
                                        )
                                    },
                                    onSwipeRight = {
                                        // Save (Copy) action
                                        pendingDeleteImage = image
                                        pendingAction = "MOVE" // Keep using "MOVE" var or rename to "SAVE", but logic below just needs a value
                                        viewModel.saveImage(
                                            image = image,
                                            onSuccess = {
                                                // Don't remove image, just reset the card position so it snaps back
                                                resetTargetId = image.id
                                                resetCardKey++
                                                
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "\"${image.name}\" saved to Scroll and Delete folder",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            },
                                            onError = {
                                                // Reset card on error too
                                                resetTargetId = image.id
                                                resetCardKey++
                                                
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "Failed to save image",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Permission Required",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Grant storage permission to view images",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { permissionLauncher.launch(permissions.toTypedArray()) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}
