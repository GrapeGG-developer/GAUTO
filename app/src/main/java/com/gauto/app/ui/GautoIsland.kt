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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Живой фон Gauto: тёмный градиент, мягкое свечение у острова и медленно
 * дрейфующие «споры». При раскрытии веера частицы реагируют сильнее.
 */
@Composable
fun GautoIslandBackground(accent: Color, expanded: Boolean, modifier: Modifier = Modifier) {
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
        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xFF070B18), Color(0xFF0B1020), Color(0xFF121B36))
            )
        )
        val glowCenter = Offset(w * 0.5f, h * 0.9f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(accent.copy(alpha = if (expanded) 0.20f else 0.12f), Color.Transparent),
                center = glowCenter,
                radius = w * 1.05f
            ),
            radius = w * 1.05f,
            center = glowCenter
        )
        val amplify = if (expanded) 1.7f else 1f
        for (i in 0 until 30) {
            val fx = ((i * 73 + 11) % 100) / 100f
            val fy = ((i * 47 + 29) % 100) / 100f
            val drift = sin((phase * 2f * PI + i * 0.71f).toFloat())
            val rise = cos((phase * 2f * PI + i * 0.43f).toFloat())
            val x = w * fx + drift * 22f * amplify
            val y = h * fy + rise * 14f * amplify
            val radius = 1.4f + 2.6f * ((i % 5) / 5f)
            drawCircle(
                color = accent.copy(alpha = 0.06f + 0.13f * (0.5f + 0.5f * drift)),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * Островок Gauto: плавающая капсула с активным правилом. По касанию из неё
 * веером вылетают категории; выбор втягивает лепесток обратно и меняет экран.
 * Никакого нижнего бара — только морфинг, пружины и живой свет.
 */
@Composable
fun GautoIsland(
    current: GautoTab,
    activeLabel: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (GautoTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val radiusPx = with(density) { 148.dp.toPx() }
    val progress = remember { Animatable(0f) }
    val breathTransition = rememberInfiniteTransition(label = "gauto-breath")
    val breath by breathTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    LaunchedEffect(expanded) {
        progress.animateTo(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    val p = progress.value.coerceIn(0f, 1f)
    val tabs = GautoTab.entries

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(258.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Лепестки-категории вылетают по дуге из капсулы.
        tabs.forEachIndexed { index, tab ->
            val angle = (-64f + index * 32f) * PI.toFloat() / 180f
            val tx = sin(angle) * radiusPx * p
            val ty = -cos(angle) * radiusPx * p
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .graphicsLayer {
                        translationX = tx
                        translationY = ty
                        alpha = p
                        scaleX = 0.55f + 0.45f * p
                        scaleY = 0.55f + 0.45f * p
                        rotationZ = (angle * 180f / PI.toFloat()) * 0.22f * p
                    }
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                tab.accent.copy(alpha = if (tab == current) 1f else 0.92f),
                                tab.accent.copy(alpha = 0.55f)
                            )
                        )
                    )
                    .clickable(enabled = p > 0.5f) { onSelect(tab) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.size(25.dp)) { drawGlyph(index, Color.White, p) }
                    Spacer(Modifier.height(3.dp))
                    Text(tab.title, color = Color.White, fontSize = 8.sp, maxLines = 1)
                }
            }
        }

        // Сама капсула-остров.
        val capsuleWidth = 208.dp + 38.dp * p
        Box(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .width(capsuleWidth)
                .height(68.dp)
                .graphicsLayer {
                    scaleX = 1f + 0.014f * breath
                    scaleY = 1f + 0.014f * breath
                }
                .clip(RoundedCornerShape(34.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF1C2748),
                            Color(0xFF342063),
                            Color(0xFF0F4652)
                        )
                    )
                )
                .clickable { onToggle() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF8B5CF6), Color(0xFF22D3EE))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(32.dp)) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.25f + 0.35f * breath),
                            radius = size.minDimension * (0.18f + 0.10f * breath)
                        )
                        drawCircle(color = Color.White, radius = size.minDimension * 0.11f)
                    }
                }
                Spacer(Modifier.width(12.dp))
                AnimatedContent(
                    targetState = current.title,
                    transitionSpec = {
                        (scaleIn(spring(stiffness = Spring.StiffnessMedium), initialScale = 0.86f) + fadeIn()) togetherWith
                            (scaleOut(targetScale = 0.9f) + fadeOut())
                    },
                    label = "islandTitle"
                ) { title ->
                    Column(Modifier.width(112.dp + 18.dp * p)) {
                        Text(
                            "Gauto · $title",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            activeLabel,
                            color = Color.White.copy(alpha = 0.66f),
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Canvas(Modifier.size(10.dp)) {
                    drawCircle(
                        color = current.accent.copy(alpha = 0.35f + 0.5f * breath),
                        radius = size.minDimension / 2f
                    )
                }
            }
        }
    }
}

/** Собственные геометрические глифы категорий — без стандартных иконок. */
private fun DrawScope.drawGlyph(index: Int, color: Color, progress: Float) {
    val s = size.minDimension
    when (index) {
        0 -> {
            drawRoundRect(
                color = color.copy(alpha = 0.9f),
                topLeft = Offset(s * 0.18f, s * 0.28f),
                size = androidx.compose.ui.geometry.Size(s * 0.64f, s * 0.52f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.12f)
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
