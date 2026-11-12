package com.gauto.app.ui

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object GautoAccents {
    val DEFAULT = listOf(
        Color(0xFF8B5CF6),
        Color(0xFFF87171),
        Color(0xFF22D3EE),
        Color(0xFF60A5FA),
        Color(0xFF34D399)
    )
}

val LocalGautoAccents = compositionLocalOf { GautoAccents.DEFAULT }
val LocalGautoMotion = compositionLocalOf { 0.35f }

private val ClearDark = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color(0xFF120A24),
    primaryContainer = Color(0xFF3B2A6B),
    onPrimaryContainer = Color(0xFFF2EDFF),
    secondary = Color(0xFF22D3EE),
    tertiary = Color(0xFF34D399),
    background = Color(0xFF080B14),
    onBackground = Color(0xFFF7F8FC),
    surface = Color(0xFF141A2A),
    onSurface = Color(0xFFF7F8FC),
    surfaceVariant = Color(0xFF1D2740),
    onSurfaceVariant = Color(0xFFC7D0E4),
    outline = Color(0xFF3B4A6B),
    error = Color(0xFFF87171)
)

/** Чёткая типографика: крупные заголовки, короткие подписи, высокий контраст. */
private val ClearTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

@Composable
fun GautoTheme(motion: Float = 0.35f, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scheme = if (dynamic) dynamicDarkColorScheme(context) else ClearDark
    val accents = if (dynamic) {
        listOf(scheme.primary, scheme.error, scheme.secondary, scheme.tertiary, scheme.primaryContainer)
    } else {
        GautoAccents.DEFAULT
    }
    CompositionLocalProvider(
        LocalGautoAccents provides accents,
        LocalGautoMotion provides motion.coerceIn(0.35f, 1f)
    ) {
        MaterialTheme(colorScheme = scheme, typography = ClearTypography, content = content)
    }
}
