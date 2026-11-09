package com.gauto.app.core

import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import kotlin.math.roundToInt

/** Системные действия GAUTO: яркость, громкость, «Не беспокоить», вибрация. */
object DeviceActions {

    fun canWriteSettings(context: Context): Boolean =
        runCatching { Settings.System.canWrite(context) }.getOrDefault(false)

    fun isAutoBrightnessOn(cr: ContentResolver): Boolean = runCatching {
        Settings.System.getInt(
            cr,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
    }.getOrDefault(false)

    fun setAutoBrightness(context: Context, enabled: Boolean): Boolean {
        if (!canWriteSettings(context)) return false
        return runCatching {
            val mode = if (enabled) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)
            true
        }.getOrDefault(false)
    }

    fun setBrightnessPercent(context: Context, percent: Int): Boolean {
        if (!canWriteSettings(context)) return false
        return runCatching {
            val cr = context.contentResolver
            if (isAutoBrightnessOn(cr)) setAutoBrightness(context, false)
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, RuleLogic.percentToBrightnessRaw(percent))
            true
        }.getOrDefault(false)
    }

    fun brightnessPercent(context: Context): Int = runCatching {
        val raw = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        (raw * 100f / 255f).roundToInt().coerceIn(0, 100)
    }.getOrDefault(0)

    fun setVolumePercent(context: Context, percent: Int): Boolean {
        val am = context.getSystemService(AudioManager::class.java) ?: return false
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val index = RuleLogic.percentToVolumeIndex(percent, max)
        return runCatching {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0)
            true
        }.getOrDefault(false)
    }

    fun volumePercent(context: Context): Int {
        val am = context.getSystemService(AudioManager::class.java) ?: return 0
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max <= 0) return 0
        return (am.getStreamVolume(AudioManager.STREAM_MUSIC) * 100f / max).roundToInt().coerceIn(0, 100)
    }

    fun dndGranted(context: Context): Boolean = runCatching {
        context.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted == true
    }.getOrDefault(false)

    fun setDnd(context: Context, on: Boolean): Boolean {
        if (!dndGranted(context)) return false
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        return runCatching {
            nm.setInterruptionFilter(
                if (on) NotificationManager.INTERRUPTION_FILTER_NONE
                else NotificationManager.INTERRUPTION_FILTER_ALL
            )
            true
        }.getOrDefault(false)
    }

    fun vibrate(context: Context, ms: Long = 80L) {
        runCatching {
            val vibrator = context.getSystemService(Vibrator::class.java) ?: return
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }
}
