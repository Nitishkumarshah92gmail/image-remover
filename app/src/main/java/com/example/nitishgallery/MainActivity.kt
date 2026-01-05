package com.example.nitishgallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.nitishgallery.ui.theme.NitishGalleryTheme
import com.example.nitishgallery.ui.NavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NitishGalleryTheme {
                NavGraph()
            }
        }
    }
}