package com.gauto.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Простой «островок»: капсула показывает один главный статус;
 * касание раскрывает 4 крупные категории. Без эффектов, требующих разбираться.
 */
@Composable
fun GautoIsland(
    current: GautoTab,
    status: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (GautoTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val accents = LocalGautoAccents.current
    val haptics = LocalHapticFeedback.current
    val radiusPx = with(density) { 158.dp.toPx() }
    val tabs = GautoTab.entries
    val progress = remember { Animatable(0f) }

    LaunchedEffect(expanded) {
        progress.animateTo(
            if (expanded) 1f else 0f,
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
        )
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    val p = progress.value.coerceIn(0f, 1f)

    Box(modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.BottomCenter) {
        tabs.forEachIndexed { index, tab ->
            val angle = (-48f + index * 32f) * PI.toFloat() / 180f
            val tx = sin(angle) * radiusPx * p
            val ty = -cos(angle) * radiusPx * p
            val color = accents.getOrElse(index) { accents.first() }
            Box(
                Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        translationX = tx
                        translationY = ty
                        alpha = p
                        scaleX = 0.78f + 0.22f * p
                        scaleY = 0.78f + 0.22f * p
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(color.copy(alpha = if (tab == current) 1f else 0.92f))
                    .clickable(enabled = p > 0.5f) { onSelect(tab) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GautoAnimatedLogo(Modifier.size(24.dp), index)
                    Spacer(Modifier.height(4.dp))
                    Text(tab.title, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }

        Box(
            Modifier
                .padding(bottom = 14.dp)
                .width(250.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF1D2740), Color(0xFF25213D))))
                .clickable { onToggle() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GautoAnimatedLogo(Modifier.size(36.dp), current.ordinal)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Gauto · ${current.title}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(status, color = Color.White.copy(alpha = 0.76f), fontSize = 11.sp, maxLines = 1)
                }
                Text(if (expanded) "×" else "⌃", color = Color.White.copy(alpha = 0.72f), fontSize = 18.sp)
            }
        }
    }
}
