package com.gauto.app.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gauto.app.R
import com.gauto.app.ui.AlarmOverlayActivity

/** Точные будильники Gauto: подъём, рассвет, таймеры и напоминания. */
object AlarmScheduler {
    const val ACTION_ALARM_FIRE = "com.gauto.app.action.ALARM_FIRE"
    const val ACTION_SUNRISE = "com.gauto.app.action.SUNRISE"
    const val ACTION_TIMER_FIRE = "com.gauto.app.action.TIMER_FIRE"
    const val ACTION_REMINDER_FIRE = "com.gauto.app.action.REMINDER_FIRE"
    const val EXTRA_ID = "alarm_id"
    const val EXTRA_MODE = "alarm_mode"

    private fun pending(context: Context, action: String, id: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            context,
            (action + id).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleAlarm(context: Context, alarm: GautoAlarm) {
        val am = context.getSystemService(android.app.AlarmManager::class.java) ?: return
        if (!alarm.enabled) {
            cancel(context, ACTION_ALARM_FIRE, alarm.id)
            cancel(context, ACTION_SUNRISE, alarm.id)
            return
        }
        val trigger = AlarmLogic.nextTrigger(alarm) ?: return
        runCatching {
            am.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                trigger.toInstant().toEpochMilli(),
                pending(context, ACTION_ALARM_FIRE, alarm.id)
            )
        }
        if (alarm.sunriseMinutes > 0) {
            val sunrise = AlarmLogic.sunriseAt(alarm)
            if (sunrise != null) {
                runCatching {
                    am.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        sunrise.toInstant().toEpochMilli(),
                        pending(context, ACTION_SUNRISE, alarm.id)
                    )
                }
            }
        }
    }

    fun scheduleSunriseStep(context: Context, alarm: GautoAlarm, atMillis: Long) {
        val am = context.getSystemService(android.app.AlarmManager::class.java) ?: return
        runCatching {
            am.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                atMillis,
                pending(context, ACTION_SUNRISE, alarm.id)
            )
        }
    }

    fun scheduleTimer(context: Context, timer: GautoTimer) {
        val am = context.getSystemService(android.app.AlarmManager::class.java) ?: return
        if (!timer.running) {
            cancel(context, ACTION_TIMER_FIRE, timer.id)
            return
        }
        runCatching {
            am.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                timer.endsAt,
                pending(context, ACTION_TIMER_FIRE, timer.id)
            )
        }
    }

    fun scheduleReminder(context: Context, reminder: GautoReminder) {
        val am = context.getSystemService(android.app.AlarmManager::class.java) ?: return
        if (!reminder.enabled) {
            cancel(context, ACTION_REMINDER_FIRE, reminder.id)
            return
        }
        val trigger = AlarmLogic.nextReminder(reminder) ?: return
        runCatching {
            am.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                trigger.toInstant().toEpochMilli(),
                pending(context, ACTION_REMINDER_FIRE, reminder.id)
            )
        }
    }

    fun cancel(context: Context, action: String, id: String) {
        runCatching {
            context.getSystemService(android.app.AlarmManager::class.java)?.cancel(pending(context, action, id))
        }
    }

    fun scheduleAll(context: Context) {
        val store = GautoStore(context)
        store.alarms().forEach { scheduleAlarm(context, it) }
        store.timers().forEach { scheduleTimer(context, it) }
        store.reminders().forEach { scheduleReminder(context, it) }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        val store = GautoStore(app)
        val id = intent?.getStringExtra(AlarmScheduler.EXTRA_ID).orEmpty()
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> AlarmScheduler.scheduleAll(app)

            AlarmScheduler.ACTION_SUNRISE -> {
                val alarm = store.alarms().firstOrNull { it.id == id } ?: return
                WakeEffects.sunrise(app, alarm)
            }

            AlarmScheduler.ACTION_ALARM_FIRE -> {
                val alarm = store.alarms().firstOrNull { it.id == id } ?: return
                WakeEffects.fire(app, alarm)
                AlarmScheduler.scheduleAlarm(app, alarm)
                AlarmRinger.start(
                    app, alarm.sound, alarm.volume, alarm.rampSec,
                    alarm.vibration, alarm.autoStopMinutes
                )
                showOverlay(app, AlarmOverlayActivity.MODE_ALARM, alarm.id, alarm.label)
            }

            AlarmScheduler.ACTION_TIMER_FIRE -> {
                val timer = store.timers().firstOrNull { it.id == id } ?: return
                store.saveTimers(store.timers().map { if (it.id == id) it.copy(running = false) else it })
                if (timer.label.startsWith("Тишина")) runCatching { DeviceActions.setDnd(app, false) }
                AlarmRinger.playShort(app, timer.sound, timer.volume, timer.vibration)
                store.event("Таймер завершён: ${timer.label}")
                showOverlay(app, AlarmOverlayActivity.MODE_TIMER, timer.id, timer.label)
            }

            AlarmScheduler.ACTION_REMINDER_FIRE -> {
                val reminder = store.reminders().firstOrNull { it.id == id } ?: return
                AlarmRinger.playShort(app, reminder.sound, 65, reminder.vibration)
                store.event("Напоминание: ${reminder.text}")
                if (reminder.once) {
                    store.saveReminders(store.reminders().map { if (it.id == id) it.copy(enabled = false) else it })
                } else {
                    AlarmScheduler.scheduleReminder(app, reminder)
                }
                showOverlay(app, AlarmOverlayActivity.MODE_REMINDER, reminder.id, reminder.text)
            }
        }
        runCatching { GautoWidget.refresh(app) }
    }

    private fun showOverlay(context: Context, mode: String, id: String, label: String) {
        runCatching { createChannel(context) }
        val overlay = Intent(context, AlarmOverlayActivity::class.java)
            .putExtra(AlarmOverlayActivity.EXTRA_MODE, mode)
            .putExtra(AlarmOverlayActivity.EXTRA_ID, id)
            .putExtra(AlarmOverlayActivity.EXTRA_LABEL, label)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(
            context, (mode + id).hashCode(), overlay,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_gauto_foreground)
            .setContentTitle("Gauto · ${if (mode == AlarmOverlayActivity.MODE_ALARM) "подъём" else label}")
            .setContentText("Откройте, чтобы подтвердить")
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(pi, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        runCatching {
            context.getSystemService(NotificationManager::class.java)?.notify(9_001, notification)
        }
        runCatching { context.startActivity(overlay) }
    }

    private fun createChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALARM, "Будильник Gauto", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(true)
                setShowBadge(false)
            }
        )
    }

    companion object {
        const val CHANNEL_ALARM = "gauto_alarm"
    }
}
