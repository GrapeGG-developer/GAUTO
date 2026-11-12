package com.gauto.app.core

import android.content.Context
import java.time.ZonedDateTime

/** Короткие действия: всё важное в один тап, без поиска по разделам. */
object QuickActions {
    const val FOCUS_MINUTES = 25
    const val QUIET_MINUTES = 30

    fun focusTimer(now: Long = System.currentTimeMillis()): GautoTimer = GautoTimer(
        label = "Фокус $FOCUS_MINUTES мин",
        durationSec = FOCUS_MINUTES * 60,
        running = true,
        endsAt = now + FOCUS_MINUTES * 60_000L,
        blockMode = BlockMode.FOCUS
    )

    fun quietTimer(now: Long = System.currentTimeMillis(), minutes: Int = QUIET_MINUTES): GautoTimer = GautoTimer(
        label = "Тишина $minutes мин",
        durationSec = minutes * 60,
        running = true,
        endsAt = now + minutes * 60_000L,
        blockMode = BlockMode.TIMER
    )

    fun sleepRule(): GautoRule = GautoRule(
        id = "quick-sleep",
        name = "Сон",
        hour = ZonedDateTime.now().hour,
        minute = ZonedDateTime.now().minute,
        days = (1..7).toList(),
        enabled = true,
        useBrightness = true,
        brightness = 12,
        useVolume = true,
        volume = 0,
        silentMode = 1,
        grayscale = true,
        blockPackages = GautoRepo.defaultBlocked(),
        packId = "quick"
    )

    fun focusSession(now: Long = System.currentTimeMillis()): FocusSession = FocusSession(
        startedAt = now,
        minutes = FOCUS_MINUTES,
        label = "фокус $FOCUS_MINUTES мин"
    )

    fun nextAlarmLabel(store: GautoStore, now: ZonedDateTime = ZonedDateTime.now()): String {
        val next = store.alarms().filter { it.enabled }
            .mapNotNull { AlarmLogic.nextTrigger(it, now) }
            .minByOrNull { it.toInstant().toEpochMilli() }
        return if (next == null) "будильников нет" else
            "подъём %02d:%02d".format(next.hour, next.minute)
    }

    fun shortStatus(store: GautoStore, now: Long = System.currentTimeMillis()): String {
        if (store.settings().activeAlarmId == "quick-sleep") return "режим сна · выйти на главном"
        if (store.settings().activeAlarmId.isNotBlank()) return "подъём · подтвердите"
        val mode = store.activeBlockMode(now)
        if (mode != BlockMode.NONE) return "блокировка: ${mode.title.lowercase()}"
        val rule = store.activeRule()
        return if (rule != null) "правило: ${rule.name}" else "всё спокойно"
    }

    fun todayHabit(store: GautoStore): Habit? {
        val today = java.time.LocalDate.now().toString()
        return store.habits().firstOrNull { today !in it.doneDates }
    }

    fun startFocus(context: Context, store: GautoStore) {
        val timer = focusTimer()
        store.upsertTimer(timer)
        store.addSession(focusSession())
        AlarmScheduler.scheduleTimer(context, timer)
        runCatching { GautoWidget.refresh(context) }
        store.event("Фокус $FOCUS_MINUTES мин · одним тапом")
    }

    fun quiet(context: Context, store: GautoStore, minutes: Int = QUIET_MINUTES) {
        val timer = quietTimer(minutes = minutes)
        store.upsertTimer(timer)
        AlarmScheduler.scheduleTimer(context, timer)
        runCatching { DeviceActions.setDnd(context, true) }
        runCatching { GautoWidget.refresh(context) }
        store.event("Тишина $minutes мин · одним тапом")
    }

    fun sleep(context: Context, store: GautoStore) {
        runCatching { DeviceActions.setDnd(context, true) }
        GrayscaleController.apply(context, true)
        store.saveSettings(store.settings().copy(activeAlarmId = "quick-sleep"))
        runCatching { GautoWidget.refresh(context) }
        store.event("Режим сна · одним тапом")
    }

    fun wake(context: Context, store: GautoStore) {
        GrayscaleController.apply(context, false)
        runCatching { DeviceActions.setDnd(context, false) }
        store.saveSettings(store.settings().copy(activeAlarmId = ""))
        runCatching { GautoWidget.refresh(context) }
        store.event("Режим сна выключен")
    }
}
