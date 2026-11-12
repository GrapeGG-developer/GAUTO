package com.gauto.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.sin

/** Простой знак Gauto: капсула и одна пульсирующая точка. Без лишних деталей. */
@Composable
fun GautoAnimatedLogo(modifier: Modifier = Modifier, activeIndex: Int = 0) {
    val accents = LocalGautoAccents.current
    val motion = LocalGautoMotion.current
    val transition = rememberInfiniteTransition(label = "logo")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )
    Canvas(modifier) {
        val s = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f
        val accent = accents.getOrElse(activeIndex) { accents.first() }
        val w = s * 0.62f
        val h = s * 0.28f
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(accent, accents.getOrElse(1) { accent })),
            topLeft = Offset(cx - w / 2f, cy - h / 2f),
            size = Size(w, h),
            cornerRadius = CornerRadius(h / 2f)
        )
        val pulse = 0.5f + 0.5f * sin((phase * 2f * PI).toFloat())
        drawCircle(
            color = Color.White.copy(alpha = 0.75f + 0.25f * pulse * motion),
            radius = s * (0.055f + 0.012f * pulse * motion),
            center = Offset(cx, cy)
        )
    }
}
