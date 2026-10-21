package com.rippipupil.clothify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.rippipupil.clothify.data.location.refreshLocation
import com.rippipupil.clothify.ui.ClothifyRoot
import com.rippipupil.clothify.ui.theme.ClothifyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ClothifyApp).container
        lifecycleScope.launch { refreshLocation(container.settings) { container.location.lastKnown() } }
        setContent {
            ClothifyTheme { ClothifyRoot(container) }
        }
    }
}
