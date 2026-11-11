package com.gauto.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.gauto.app.core.AlarmLogic
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 4D-слой Gauto: три оси глубины (cameraDistance + rotationX/Y + масштаб)
 * плюс четвёртая ось — время. Фаза дня и фаза анимации вместе управляют
 * наклоном, цветом и скоростью колец, поэтому интерфейс «живёт» в сутках,
 * а не просто крутится.
 */
@Composable
fun Gauto4DBackdrop(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    phase: Float = AlarmLogic.phaseOfDay(LocalTime.now())
) {
    val accents = LocalGautoAccents.current
    val motion = LocalGautoMotion.current
    val density = LocalDensity.current
    val camera = with(density) { 460.dp.toPx() }
    val transition = rememberInfiniteTransition(label = "gauto-4d")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(19_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "timeAxis"
    )

    Box(modifier.fillMaxSize()) {
        for (layer in 0 until 4) {
            val layerPhase = (phase + layer * 0.11f + time * 0.06f) % 1f
            val tilt = sin((layerPhase * 2f * PI).toFloat()) * (7f + layer * 2f) * motion
            val turn = cos((layerPhase * 2f * PI).toFloat()) * (9f + layer * 2f) * motion
            val depth = 0.72f + layer * 0.09f + (if (expanded) 0.08f else 0f)
            val color = accents.getOrElse(layer) { accents.first() }
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        cameraDistance = camera
                        rotationX = tilt
                        rotationY = turn
                        scaleX = depth
                        scaleY = depth
                        alpha = (0.10f + layer * 0.025f) * motion
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension * (0.24f + layer * 0.10f)
                    val cx = size.width * (0.5f + 0.06f * sin((phase * 2f * PI + layer).toFloat()))
                    val cy = size.height * (0.46f + 0.05f * cos((phase * 2f * PI + layer).toFloat()))
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(color.copy(alpha = 0.55f), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = r
                        ),
                        radius = r,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = color.copy(alpha = 0.32f),
                        radius = r * 0.82f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2f + layer)
                    )
                }
            }
        }
    }
}
