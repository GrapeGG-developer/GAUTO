package com.gauto.app.ui

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gauto.app.core.AlarmLogic
import com.gauto.app.core.AlarmRinger
import com.gauto.app.core.GautoStore
import com.gauto.app.core.WakeEffects
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/**
 * Полноэкранный оверлей подъёма Gauto.
 *
 * Здесь намеренно нет переноса сигнала и кнопок задержки: звонок прекращается
 * только осознанным долгим подтверждением «Я встал». Визуал — единый
 * 4D Living Island с рассветом, частицами и живым логотипом.
 */
class AlarmOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val mode = intent?.getStringExtra(EXTRA_MODE).orEmpty().ifBlank { MODE_ALARM }
        val id = intent?.getStringExtra(EXTRA_ID).orEmpty()
        val label = intent?.getStringExtra(EXTRA_LABEL).orEmpty().ifBlank { "Подъём" }

        setContent {
            GautoTheme {
                AlarmOverlayScreen(
                    mode = mode,
                    label = label,
                    onConfirm = {
                        confirm(applicationContext, mode, id)
                        finish()
                    }
                )
            }
        }
    }

    private fun confirm(context: Context, mode: String, id: String) {
        val store = GautoStore(context)
        val alarm = if (mode == MODE_ALARM) store.alarms().firstOrNull { it.id == id } else null
        WakeEffects.confirm(context, alarm)
        AlarmRinger.stop()
        runCatching {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancel(9_001)
        }
    }

    companion object {
        const val MODE_ALARM = "alarm"
        const val MODE_TIMER = "timer"
        const val MODE_REMINDER = "reminder"
        const val EXTRA_MODE = "mode"
        const val EXTRA_ID = "id"
        const val EXTRA_LABEL = "label"
    }
}

@Composable
private fun AlarmOverlayScreen(mode: String, label: String, onConfirm: () -> Unit) {
    BackHandler(enabled = true) { /* уйти можно только подтверждением */ }
    val accents = LocalGautoAccents.current
    var started by remember { androidx.compose.runtime.mutableStateOf(false) }
    val dawn by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(3_000, easing = LinearEasing),
        label = "dawn"
    )
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val breathTransition = rememberInfiniteTransition(label = "overlay-breath")
    val breath by breathTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_600, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "breath"
    )
    LaunchedEffect(Unit) {
        started = true
        while (isActive) {
            delay(1_000L)
            now = System.currentTimeMillis()
        }
    }
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
    val date = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now))
    val title = when (mode) {
        AlarmOverlayActivity.MODE_TIMER -> "Таймер завершён"
        AlarmOverlayActivity.MODE_REMINDER -> label
        else -> label
    }
    val button = when (mode) {
        AlarmOverlayActivity.MODE_TIMER -> "Готово"
        AlarmOverlayActivity.MODE_REMINDER -> "Принято"
        else -> "Я встал"
    }
    val primary = accents.getOrElse(0) { accents.first() }
    val secondary = accents.getOrElse(1) { accents.first() }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF05070F).copy(alpha = 1f - 0.35f * dawn),
                        Color(0xFF0B1020),
                        primary.copy(alpha = 0.10f + 0.32f * dawn)
                    )
                )
            )
    ) {
        Gauto4DBackdrop(expanded = false)
        Canvas(Modifier.fillMaxSize()) {
            for (i in 0 until 46) {
                val fx = ((i * 71 + 13) % 100) / 100f
                val fy = ((i * 53 + 29) % 100) / 100f
                val drift = sin((breath * 2f * PI + i * 0.6f).toFloat())
                drawCircle(
                    color = secondary.copy(alpha = (0.05f + 0.16f * dawn) * (0.5f + 0.5f * drift)),
                    radius = 1.5f + 3f * ((i % 5) / 5f),
                    center = Offset(size.width * fx + drift * 18f, size.height * fy)
                )
            }
        }

        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))
            GautoAnimatedLogo(Modifier.size(92.dp), activeIndex = 0)
            Spacer(Modifier.height(18.dp))
            Text(
                time,
                color = Color.White,
                fontSize = 76.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp
            )
            Text(date, color = Color.White.copy(alpha = 0.62f), fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                title,
                color = primary.copy(alpha = 0.9f),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                AlarmLogic.sceneTitles(AlarmLogic.phaseOfDay()) + " · рассвет " + (dawn * 100).toInt() + "%",
                color = Color.White.copy(alpha = 0.42f),
                fontSize = 11.sp
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(216.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                primary.copy(alpha = 0.75f + 0.25f * breath),
                                secondary.copy(alpha = 0.62f + 0.28f * dawn)
                            )
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { onConfirm() })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    button,
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "удерживайте, чтобы подтвердить",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(52.dp))
        }
    }
}
