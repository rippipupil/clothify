package com.rippipupil.clothify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rippipupil.clothify.ui.ClothifyRoot
import com.rippipupil.clothify.ui.theme.ClothifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ClothifyApp).container
        setContent {
            ClothifyTheme { ClothifyRoot(container) }
        }
    }
}
