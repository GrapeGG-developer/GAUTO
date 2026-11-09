package com.gauto.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GautoDark = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B2A6B),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFF22D3EE),
    onSecondary = Color(0xFF062028),
    background = Color(0xFF0B1020),
    onBackground = Color(0xFFF4F4FB),
    surface = Color(0xFF151B2E),
    onSurface = Color(0xFFF4F4FB),
    surfaceVariant = Color(0xFF1E2740),
    onSurfaceVariant = Color(0xFFB9C2DA),
    outline = Color(0xFF3A4666),
    error = Color(0xFFF87171)
)

@Composable
fun GautoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GautoDark,
        content = content
    )
}
