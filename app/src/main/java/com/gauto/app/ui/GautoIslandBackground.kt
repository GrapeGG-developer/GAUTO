package com.gauto.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Спокойный общий фон: чистый градиент + одно мягкое цветовое пятно. */
@Composable
fun GautoIslandBackground(accent: Color, expanded: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF080B14), Color(0xFF0C1120), Color(0xFF141A2A))))
        val center = Offset(size.width * 0.5f, size.height * 0.9f)
        val radius = size.width * 0.85f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(accent.copy(alpha = if (expanded) 0.13f else 0.07f), Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}
