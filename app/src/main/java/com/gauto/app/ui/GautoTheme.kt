package com.gauto.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val GautoDark = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B2A6B),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFF22D3EE),
    onSecondary = Color(0xFF062028),
    tertiary = Color(0xFF34D399),
    background = Color(0xFF070B18),
    onBackground = Color(0xFFF4F4FB),
    surface = Color(0xFF121B36),
    onSurface = Color(0xFFF4F4FB),
    surfaceVariant = Color(0xFF1B2745),
    onSurfaceVariant = Color(0xFFB9C2DA),
    outline = Color(0xFF3A4666),
    error = Color(0xFFF87171)
)

private val GautoTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 23.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp)
)

@Composable
fun GautoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GautoDark,
        typography = GautoTypography,
        content = content
    )
}
