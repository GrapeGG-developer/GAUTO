package com.gauto.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Живой фон Gauto: градиент, свечение у острова, два слоя дрейфующих частиц,
 * хвосты и волна раскрытия. Амплитуда зависит от «живости» в настройках.
 */
@Composable
fun GautoIslandBackground(accent: Color, expanded: Boolean, modifier: Modifier = Modifier) {
    val motion = LocalGautoMotion.current
    val transition = rememberInfiniteTransition(label = "gauto-spores")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sporePhase"
    )
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(0xFF070B18), Color(0xFF0B1020), Color(0xFF121B36))))
        val glowCenter = Offset(w * 0.5f, h * 0.9f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(accent.copy(alpha = (if (expanded) 0.24f else 0.13f) * motion), Color.Transparent),
                center = glowCenter,
                radius = w * 1.05f
            ),
            radius = w * 1.05f,
            center = glowCenter
        )
        val amplify = (1f + 0.9f * (if (expanded) 1f else 0f)) * motion
        for (i in 0 until 42) {
            val fx = ((i * 73 + 11) % 100) / 100f
            val fy = ((i * 47 + 29) % 100) / 100f
            val drift = sin((phase * 2f * PI + i * 0.71f).toFloat())
            val rise = cos((phase * 2f * PI + i * 0.43f).toFloat())
            val x = w * fx + drift * 24f * amplify
            val y = h * fy + rise * 15f * amplify
            val radius = 1.2f + 2.8f * ((i % 5) / 5f)
            drawCircle(
                color = accent.copy(alpha = (0.05f + 0.15f * (0.5f + 0.5f * drift)) * motion),
                radius = radius,
                center = Offset(x, y)
            )
            if (expanded && i % 6 == 0) {
                drawLine(
                    color = accent.copy(alpha = 0.08f * motion),
                    start = Offset(x, y),
                    end = Offset(x - drift * 26f * motion, y + rise * 16f * motion),
                    strokeWidth = 1.2f
                )
            }
        }
    }
}

/**
 * Островок Gauto 3.0: капсула с живой каруселью статусов, анимированным
 * логотипом и веером категорий с настоящим stagger-появлением каждого лепестка.
 */
@Composable
fun GautoIsland(
    current: GautoTab,
    statuses: List<String>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (GautoTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val accents = LocalGautoAccents.current
    val motion = LocalGautoMotion.current
    val haptics = LocalHapticFeedback.current
    val radiusPx = with(density) { 172.dp.toPx() }
    val tabs = GautoTab.entries

    val capsuleProgress = remember { Animatable(0f) }
    val petalProgress = remember { List(tabs.size) { Animatable(0f) } }
    val breathTransition = rememberInfiniteTransition(label = "gauto-breath")
    val breath by breathTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )
    val waveTransition = rememberInfiniteTransition(label = "gauto-wave")
    val wave by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    LaunchedEffect(expanded) {
        launch {
            capsuleProgress.animateTo(
                if (expanded) 1f else 0f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }
        petalProgress.forEachIndexed { index, anim ->
            launch {
                delay((if (expanded) index else (tabs.size - 1 - index)) * 48L)
                anim.animateTo(
                    if (expanded) 1f else 0f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )
            }
        }
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    var statusIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(statuses) {
        if (statuses.size <= 1) return@LaunchedEffect
        while (isActive) {
            delay(3_000L)
            statusIndex = (statusIndex + 1) % statuses.size
        }
    }
    val status = statuses.getOrElse(statusIndex) { "Gauto" }

    val p = capsuleProgress.value.coerceIn(0f, 1f)

    Box(
        modifier = modifier.fillMaxWidth().height(264.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Волна раскрытия под лепестками.
        if (p > 0.01f) {
            Canvas(
                Modifier
                    .size(300.dp)
                    .graphicsLayer { alpha = (1f - wave) * 0.45f * p * motion }
            ) {
                drawCircle(
                    color = accents.getOrElse(current.ordinal) { accents.first() },
                    radius = size.minDimension * (0.16f + 0.32f * wave),
                    style = Stroke(width = 2.5f)
                )
            }
        }

        tabs.forEachIndexed { index, tab ->
            val pp = petalProgress[index].value.coerceIn(0f, 1f)
            if (pp < 0.005f && p < 0.005f) return@forEachIndexed
            val angle = (-78f + index * (156f / (tabs.size - 1).coerceAtLeast(1))) * PI.toFloat() / 180f
            val wobble = sin((wave * 2f * PI + index * 0.9f).toFloat()) * 5f * motion * pp
            val tx = sin(angle) * radiusPx * pp + wobble
            val ty = -cos(angle) * radiusPx * pp
            val color = accents.getOrElse(index) { accents.first() }
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .graphicsLayer {
                        translationX = tx
                        translationY = ty
                        alpha = pp
                        scaleX = (0.5f + 0.5f * pp) * (1f + 0.03f * wobble / 10f)
                        scaleY = (0.5f + 0.5f * pp) * (1f + 0.03f * wobble / 10f)
                        rotationZ = (angle * 180f / PI.toFloat()) * 0.22f * pp + wobble * 0.4f
                    }
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                color.copy(alpha = if (tab == current) 1f else 0.92f),
                                color.copy(alpha = 0.55f)
                            )
                        )
                    )
                    .clickable(enabled = pp > 0.5f) { onSelect(tab) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.size(22.dp)) { drawGlyph(index, Color.White, pp) }
                    Spacer(Modifier.height(3.dp))
                    Text(tab.title, color = Color.White, fontSize = 7.sp, maxLines = 1)
                }
            }
        }

        val capsuleWidth = 210.dp + 40.dp * p
        Box(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .width(capsuleWidth)
                .height(68.dp)
                .graphicsLayer {
                    scaleX = 1f + 0.018f * breath * motion
                    scaleY = 1f + 0.018f * breath * motion
                }
                .clip(RoundedCornerShape(34.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1C2748), Color(0xFF342063), Color(0xFF0F4652))
                    )
                )
                .clickable { onToggle() }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GautoAnimatedLogo(
                    modifier = Modifier.size(34.dp),
                    activeIndex = current.ordinal
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.width(118.dp + 20.dp * p)) {
                    Text(
                        "Gauto · ${current.title}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    AnimatedContent(
                        targetState = status,
                        transitionSpec = {
                            (slideInVertically { it / 2 } + fadeIn(tween(220))) togetherWith
                                (slideOutVertically { -it / 2 } + fadeOut(tween(180)))
                        },
                        label = "statusCarousel"
                    ) { text ->
                        Text(
                            text,
                            color = Color.White.copy(alpha = 0.68f),
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Canvas(Modifier.size(10.dp)) {
                    drawCircle(
                        color = accents.getOrElse(current.ordinal) { accents.first() }
                            .copy(alpha = 0.35f + 0.5f * breath * motion),
                        radius = size.minDimension / 2f
                    )
                }
            }
        }
    }
}

/** Собственные геометрические глифы категорий. */
private fun DrawScope.drawGlyph(index: Int, color: Color, progress: Float) {
    val s = size.minDimension
    when (index) {
        0 -> {
            drawRoundRect(
                color = color.copy(alpha = 0.9f),
                topLeft = Offset(s * 0.18f, s * 0.28f),
                size = Size(s * 0.64f, s * 0.52f),
                cornerRadius = CornerRadius(s * 0.12f)
            )
            drawCircle(color = Color(0xFF221544), radius = s * 0.09f, center = Offset(s * 0.5f, s * 0.54f))
        }
        1 -> {
            for (k in 0..2) {
                val y = s * (0.28f + k * 0.22f)
                drawLine(
                    color = color.copy(alpha = 0.92f),
                    start = Offset(s * 0.2f, y),
                    end = Offset(s * (0.78f - k * 0.1f), y),
                    strokeWidth = s * 0.09f
                )
            }
        }
        2 -> {
            drawLine(color.copy(alpha = 0.92f), Offset(s * 0.2f, s * 0.52f), Offset(s * 0.43f, s * 0.74f), s * 0.11f)
            drawLine(color.copy(alpha = 0.92f), Offset(s * 0.43f, s * 0.74f), Offset(s * 0.82f, s * 0.26f), s * 0.11f)
        }
        3 -> {
            drawCircle(color = color.copy(alpha = 0.9f), radius = s * 0.33f, style = Stroke(width = s * 0.09f))
            drawLine(color.copy(alpha = 0.9f), Offset(s * 0.5f, s * 0.5f), Offset(s * 0.5f, s * 0.28f), s * 0.08f)
            drawLine(color.copy(alpha = 0.9f), Offset(s * 0.5f, s * 0.5f), Offset(s * 0.68f, s * 0.58f), s * 0.08f)
        }
        else -> {
            drawCircle(color = color.copy(alpha = 0.9f), radius = s * 0.30f, style = Stroke(width = s * 0.09f))
            drawCircle(color = color.copy(alpha = 0.9f), radius = s * 0.10f)
        }
    }
}
