package com.example.nitishgallery.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Represents the different screens in the app navigation.
 */
sealed class Screen {
    /** The main gallery screen showing all images. */
    data object Gallery : Screen()
    
    /** The detail screen for viewing a single image at full size. */
    data class Detail(val index: Int) : Screen()
}

/**
 * Main navigation graph composable that handles screen transitions.
 * Uses animated content transitions for smooth navigation between screens.
 */
@Composable
fun NavGraph() {
    val viewModel: GalleryViewModel = viewModel()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Gallery) }
    val galleryListState = rememberLazyListState()

    Column {
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    if (targetState is Screen.Detail && initialState is Screen.Gallery) {
                        // Gallery -> Detail: Slide in from right
                        slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                                slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                    } else if (targetState is Screen.Gallery && initialState is Screen.Detail) {
                        // Detail -> Gallery: Slide in from left
                        slideInHorizontally(initialOffsetX = { -it }) + fadeIn() togetherWith
                                slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                    } else {
                        fadeIn() togetherWith fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.Gallery -> {
                        GalleryScreen(
                            viewModel = viewModel,
                            listState = galleryListState,
                            onImageClick = { index ->
                                currentScreen = Screen.Detail(index)
                            }
                        )
                    }
                    is Screen.Detail -> {
                        DetailScreen(
                            viewModel = viewModel,
                            initialIndex = screen.index,
                            onBack = {
                                currentScreen = Screen.Gallery
                            }
                        )
                    }
                }
            }
        }
        AdBanner()
    }
}

