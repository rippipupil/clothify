package com.rippipupil.clothify.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Coral = Color(0xFFFF7A59)
private val Ink = Color(0xFF1F1B2E)

private val LightColors = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    secondary = Ink,
    onSecondary = Color.White,
    background = Color(0xFFFAF8F6),
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    secondary = Color(0xFFE6E1F0),
    background = Color(0xFF15121F),
    surface = Ink,
)

@Composable
fun ClothifyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
