package com.gauto.app.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.sin

/**
 * Звук Gauto: собственный синтезатор на AudioTrack с назначением USAGE_ALARM.
 *
 * Взято лучшее из GSYNK ToneSynth (живой тон без щелчков), но добавлено
 * нарастание громкости по времени, мелодии пробуждения и честный авто-стоп.
 * Никакого «отложить на 10 минут»: сигнал либо подтверждён, либо завершён.
 */
object AlarmRinger {

    private const val SAMPLE_RATE = 44_100
    private const val FRAMES = 1024

    @Volatile private var running = false
    @Volatile private var amplitude = 0.15f
    @Volatile private var frequency = 440.0
    @Volatile private var sound = AlarmRules.SOUND_SUNRISE
    private var track: AudioTrack? = null
    private var worker: Thread? = null
    private var vibrator: Vibrator? = null

    val isPlaying: Boolean get() = running

    fun start(
        context: Context,
        sound: String,
        volume: Int,
        rampSec: Int,
        vibration: Boolean,
        autoStopMinutes: Int
    ) {
        stop()
        this.sound = if (sound in AlarmRules.SOUNDS) sound else AlarmRules.SOUND_SUNRISE
        if (this.sound == AlarmRules.SOUND_SILENT && !vibration) return
        amplitude = 0.12f
        frequency = 330.0
        val built = runCatching { buildTrack() }.getOrNull() ?: return
        track = built
        running = true
        if (vibration) startVibration(context)
        worker = Thread({
            val started = System.currentTimeMillis()
            var frame = 0L
            val buffer = ShortArray(FRAMES)
            while (running) {
                val elapsedSec = ((System.currentTimeMillis() - started) / 1000L).toInt()
                val target = AlarmLogic.escalationVolume(elapsedSec, rampSec, volume)
                val wave = waveFrequency(elapsedSec)
                frequency = wave.first
                amplitude = (target / 100f) * wave.second * 0.85f
                if (this.sound != AlarmRules.SOUND_SILENT) {
                    for (i in 0 until FRAMES) {
                        val t = (frame + i).toDouble() / SAMPLE_RATE
                        buffer[i] = (sin(2.0 * PI * frequency * t) * amplitude * Short.MAX_VALUE).toInt().toShort()
                    }
                    runCatching { built.write(buffer, 0, FRAMES) }
                }
                frame += FRAMES
                if (AlarmLogic.autoStopReached(elapsedSec, autoStopMinutes)) {
                    stop()
                    GautoStore(context).event("Будильник завершён авто-стопом (без отложить)")
                    break
                }
                runCatching { Thread.sleep(12L) }
            }
        }, "gauto-alarm-tone").apply { isDaemon = true; priority = Thread.NORM_PRIORITY + 1; start() }
    }

    /** Короткий сигнал для таймера/напоминания. */
    fun playShort(context: Context, sound: String, volume: Int, vibration: Boolean) {
        stop()
        this.sound = if (sound in AlarmRules.SOUNDS) sound else AlarmRules.SOUND_CHIME
        amplitude = (volume.coerceIn(1, 100) / 100f) * 0.7f
        frequency = 660.0
        val built = runCatching { buildTrack() }.getOrNull() ?: return
        track = built
        running = true
        if (vibration) runCatching { vibrateOnce(context) }
        worker = Thread({
            val buffer = ShortArray(FRAMES)
            val started = System.currentTimeMillis()
            var frame = 0L
            while (running && System.currentTimeMillis() - started < 2_200L) {
                for (i in 0 until FRAMES) {
                    val t = (frame + i).toDouble() / SAMPLE_RATE
                    val pulse = if ((System.currentTimeMillis() - started) % 500L < 300L) 1f else 0.15f
                    buffer[i] = (sin(2.0 * PI * frequency * t) * amplitude * pulse * Short.MAX_VALUE).toInt().toShort()
                }
                runCatching { built.write(buffer, 0, FRAMES) }
                frame += FRAMES
            }
            stop()
        }, "gauto-short-tone").apply { isDaemon = true; start() }
    }

    private fun waveFrequency(elapsedSec: Int): Pair<Double, Float> = when (sound) {
        AlarmRules.SOUND_SUNRISE -> {
            val t = (elapsedSec % 8) / 8f
            (330.0 + 330.0 * t) to (0.55f + 0.45f * t)
        }
        AlarmRules.SOUND_PULSE -> {
            val beat = if (elapsedSec % 2 == 0) 1f else 0.25f
            440.0 to beat
        }
        AlarmRules.SOUND_CHIME -> (if (elapsedSec % 2 == 0) 784.0 else 988.0) to 0.9f
        AlarmRules.SOUND_BELL -> 523.0 to 1f
        else -> 440.0 to 0f
    }

    fun stop() {
        running = false
        runCatching { vibrator?.cancel() }
        vibrator = null
        val current = track
        track = null
        runCatching { worker?.join(220) }
        worker = null
        runCatching { current?.pause() }
        runCatching { current?.flush() }
        runCatching { current?.stop() }
        runCatching { current?.release() }
    }

    private fun buildTrack(): AudioTrack {
        val minBytes = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(FRAMES * 2)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build().also { it.play() }
    }

    private fun systemVibrator(context: Context): Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    private fun startVibration(context: Context) {
        vibrator = systemVibrator(context)
        val v = vibrator ?: return
        runCatching {
            val effect = VibrationEffect.createWaveform(longArrayOf(0, 420, 380, 420, 900), intArrayOf(0, 180, 0, 220, 0), 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val attrs = VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_ALARM).build()
                v.vibrate(effect, attrs)
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(effect)
            }
        }
    }

    private fun vibrateOnce(context: Context) {
        val v = systemVibrator(context) ?: return
        runCatching { v.vibrate(VibrationEffect.createOneShot(320L, VibrationEffect.DEFAULT_AMPLITUDE)) }
    }
}

/** Световые эффекты пробуждения: рассвет, снятие ч/б и разблокировка. */
object WakeEffects {
    /** Начинает плавный подъём яркости и ставит следующий 60-секундный шаг. */
    fun sunrise(context: Context, alarm: GautoAlarm) {
        val store = GautoStore(context)
        val now = System.currentTimeMillis()
        var settings = store.settings()
        if (!settings.autoBrightnessSaved) {
            settings = settings.copy(
                autoBrightnessSaved = true,
                autoBrightnessWasOn = DeviceActions.isAutoBrightnessOn(context.contentResolver),
                wakeBrightnessStart = DeviceActions.brightnessPercent(context),
                wakeSunriseStartedAt = now
            )
            store.saveSettings(settings)
        }
        val trigger = AlarmLogic.nextTrigger(alarm)
        val totalMs = alarm.sunriseMinutes.coerceAtLeast(1) * 60_000L
        val elapsed = (now - settings.wakeSunriseStartedAt).coerceAtLeast(0L)
        val progress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
        val start = settings.wakeBrightnessStart.coerceIn(1, 100)
        val target = alarm.wakeBrightness.coerceIn(1, 100)
        val current = (start + (target - start) * progress).toInt().coerceIn(1, 100)
        runCatching { DeviceActions.setBrightnessPercent(context, current) }
        if (alarm.disableGrayscale) GrayscaleController.apply(context, false)
        if (trigger != null) {
            val nextStep = now + 60_000L
            val triggerMs = trigger.toInstant().toEpochMilli()
            if (nextStep < triggerMs) AlarmScheduler.scheduleSunriseStep(context, alarm, nextStep)
        }
        store.event("Рассвет: ${alarm.label} · яркость $current%")
    }

    fun fire(context: Context, alarm: GautoAlarm) {
        val store = GautoStore(context)
        var settings = store.settings()
        if (!settings.autoBrightnessSaved) {
            settings = settings.copy(
                autoBrightnessSaved = true,
                autoBrightnessWasOn = DeviceActions.isAutoBrightnessOn(context.contentResolver),
                wakeBrightnessStart = DeviceActions.brightnessPercent(context),
                wakeSunriseStartedAt = System.currentTimeMillis()
            )
        }
        store.saveSettings(settings.copy(activeAlarmId = alarm.id))
        runCatching { DeviceActions.setBrightnessPercent(context, alarm.wakeBrightness) }
        if (alarm.disableGrayscale) GrayscaleController.apply(context, false)
        runCatching { DeviceActions.setDnd(context, false) }
        store.event("Будильник: ${alarm.label} · ${alarm.timeLabel}")
    }

    fun confirm(context: Context, alarm: GautoAlarm?) {
        AlarmRinger.stop()
        val store = GautoStore(context)
        val settings = store.settings()
        if (settings.autoBrightnessSaved) {
            if (settings.autoBrightnessWasOn) {
                DeviceActions.setAutoBrightness(context, true)
            } else {
                DeviceActions.setBrightnessPercent(context, settings.wakeBrightnessStart)
            }
        }
        store.saveSettings(
            settings.copy(activeAlarmId = "", autoBrightnessSaved = false, wakeSunriseStartedAt = 0L)
        )
        if (alarm?.unlockApps != false) GrayscaleController.apply(context, false)
        if (alarm != null) runCatching { DeviceActions.setDnd(context, false) }
        runCatching { GautoWidget.refresh(context) }
        store.event("Подъём подтверждён · блокировки сняты")
        runCatching { AlarmScheduler.scheduleAll(context) }
    }
}
