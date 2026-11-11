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
import kotlin.math.cos
import kotlin.math.sin

/**
 * Живой логотип Gauto: та же капсула-остров, но лепестки дышат и вылетают
 * по очереди, а ядро пульсирует. Используется в шапке и внутри островка.
 */
@Composable
fun GautoAnimatedLogo(
    modifier: Modifier = Modifier,
    activeIndex: Int = 0
) {
    val accents = LocalGautoAccents.current
    val motion = LocalGautoMotion.current
    val transition = rememberInfiniteTransition(label = "gauto-logo")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logoPhase"
    )

    Canvas(modifier) {
        val s = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f
        val active = accents.getOrElse(activeIndex) { accents.first() }

        drawCircle(
            brush = Brush.radialGradient(
                listOf(active.copy(alpha = 0.32f * motion), Color.Transparent),
                center = Offset(cx, cy),
                radius = s * 0.52f
            ),
            radius = s * 0.52f,
            center = Offset(cx, cy)
        )

        for (i in 0 until 5) {
            val angle = (-64f + i * 32f) * PI.toFloat() / 180f
            val wave = (sin((phase * 2f * PI + i * 0.8f).toFloat()) + 1f) / 2f
            val reach = s * (0.20f + 0.10f * wave * motion)
            val x = cx + sin(angle) * reach * 0.72f
            val y = cy - cos(angle) * reach * 0.72f
            val radius = s * (0.055f + 0.028f * wave * motion)
            val color = accents.getOrElse(i) { accents.first() }
            drawCircle(color.copy(alpha = 0.55f + 0.4f * wave), radius = radius, center = Offset(x, y))
        }

        val capsuleWidth = s * 0.50f
        val capsuleHeight = s * 0.20f
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(accents.getOrElse(0) { Color(0xFF8B5CF6) }, accents.getOrElse(1) { Color(0xFF22D3EE) }),
                start = Offset(cx - capsuleWidth / 2f, cy),
                end = Offset(cx + capsuleWidth / 2f, cy)
            ),
            topLeft = Offset(cx - capsuleWidth / 2f, cy - capsuleHeight / 2f),
            size = Size(capsuleWidth, capsuleHeight),
            cornerRadius = CornerRadius(capsuleHeight / 2f)
        )

        val corePulse = (sin((phase * 4f * PI).toFloat()) + 1f) / 2f
        drawCircle(
            color = Color.White.copy(alpha = 0.75f + 0.25f * corePulse),
            radius = s * (0.032f + 0.014f * corePulse * motion),
            center = Offset(cx, cy)
        )
    }
}
