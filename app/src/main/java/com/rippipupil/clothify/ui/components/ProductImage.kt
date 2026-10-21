package com.rippipupil.clothify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage

/** Foto de la prenda con una imagen genérica si no carga. */
@Composable
fun ProductImage(url: String, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        error = { Placeholder() },
        loading = { Placeholder() },
    )
}

@Composable
private fun Placeholder() {
    Box(Modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.Checkroom, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
