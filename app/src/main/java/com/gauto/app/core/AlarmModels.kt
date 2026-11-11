package com.gauto.app.core

import kotlinx.serialization.Serializable
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Gauto 4.0 — настоящий будильник, таймеры и напоминания.
 *
 * Ключевое правило продукта: на главном оверлее НЕТ кнопки «отложить на 10 минут».
 * Вообще нет откладывания: сигнал прекращается только по осознанному «Я встал»
 * или по честному авто-стопу (чтобы телефон не орал вечно). Авто-стоп помечает
 * пропуск, а не переносит будильник.
 */
object AlarmRules {
    const val NO_SNOOZE = true
    const val MIN_RAMP_SEC = 5
    const val MAX_RAMP_SEC = 120
    const val MIN_SUNRISE_MIN = 0
    const val MAX_SUNRISE_MIN = 30

    const val SOUND_SUNRISE = "sunrise"
    const val SOUND_PULSE = "pulse"
    const val SOUND_CHIME = "chime"
    const val SOUND_BELL = "bell"
    const val SOUND_SILENT = "silent"

    val SOUNDS = listOf(SOUND_SUNRISE, SOUND_PULSE, SOUND_CHIME, SOUND_BELL, SOUND_SILENT)
    val SOUND_LABELS = listOf("рассвет", "пульс", "колокольчик", "гудок", "тихо")

    fun soundLabel(sound: String): String =
        SOUND_LABELS.getOrElse(SOUNDS.indexOf(sound)) { "рассвет" }
}

@Serializable
enum class BlockMode(val title: String) {
    NONE("без блокировки"),
    APP("приложения"),
    SLEEP("сон"),
    FOCUS("фокус"),
    ALARM("до подъёма"),
    TIMER("на таймер"),
    REMINDER("напоминание")
}

@Serializable
data class GautoAlarm(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "Подъём",
    val hour: Int = 7,
    val minute: Int = 0,
    val days: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val enabled: Boolean = true,
    /** Всегда true. Откладывания в Gauto нет по замыслу. */
    val strict: Boolean = true,
    val sound: String = AlarmRules.SOUND_SUNRISE,
    val volume: Int = 80,
    val rampSec: Int = 30,
    val vibration: Boolean = true,
    /** «Рассвет»: за N минут начинаем поднимать яркость и снимать ч/б слой. */
    val sunriseMinutes: Int = 10,
    /** Не snooze: через N минут сигнал просто завершается и помечается пропуском. */
    val autoStopMinutes: Int = 5,
    val wakeBrightness: Int = 85,
    val disableGrayscale: Boolean = true,
    val unlockApps: Boolean = true,
    val scene: String = "dawn",
    val lastFiredDate: String = ""
) {
    val timeLabel: String get() = "%02d:%02d".format(hour, minute)
    fun daysLabel(): String = when {
        days.toSet() == (1..7).toSet() -> "Каждый день"
        days.toSet() == setOf(1, 2, 3, 4, 5) -> "По будням"
        days.toSet() == setOf(6, 7) -> "По выходным"
        days.isEmpty() -> "Дни не выбраны"
        else -> days.sorted().joinToString(" ") { RuleLogic.DAY_SHORT[it - 1] }
    }
}

@Serializable
data class GautoTimer(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "Таймер",
    val durationSec: Int = 25 * 60,
    val endsAt: Long = 0L,
    val running: Boolean = false,
    val sound: String = AlarmRules.SOUND_CHIME,
    val volume: Int = 70,
    val vibration: Boolean = true,
    val blockMode: BlockMode = BlockMode.TIMER,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun remaining(now: Long = System.currentTimeMillis()): Int =
        if (!running) durationSec else ((endsAt - now) / 1000L).toInt().coerceAtLeast(0)
}

@Serializable
data class GautoReminder(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "Напоминание",
    val hour: Int = 12,
    val minute: Int = 0,
    val days: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val enabled: Boolean = true,
    val once: Boolean = false,
    val sound: String = AlarmRules.SOUND_CHIME,
    val vibration: Boolean = true,
    val lastFiredDate: String = ""
) {
    val timeLabel: String get() = "%02d:%02d".format(hour, minute)
}

/** Чистая математика будильника: без Android, полностью покрыта тестами. */
object AlarmLogic {

    /** Следующее срабатывание с учётом дней недели. */
    fun nextTrigger(alarm: GautoAlarm, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? {
        if (!alarm.enabled) return null
        val days = alarm.days.filter { it in 1..7 }.toSet()
        if (days.isEmpty()) return null
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (days.contains(date.dayOfWeek.value)) {
                val candidate = date.atTime(alarm.hour.coerceIn(0, 23), alarm.minute.coerceIn(0, 59)).atZone(now.zone)
                if (candidate.isAfter(now)) return candidate
            }
        }
        return null
    }

    fun nextReminder(reminder: GautoReminder, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? {
        if (!reminder.enabled) return null
        val days = reminder.days.filter { it in 1..7 }.toSet()
        if (days.isEmpty()) return null
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (days.contains(date.dayOfWeek.value)) {
                val candidate = date.atTime(reminder.hour.coerceIn(0, 23), reminder.minute.coerceIn(0, 59)).atZone(now.zone)
                if (candidate.isAfter(now)) return candidate
            }
        }
        return null
    }

    /** Момент начала «рассвета» — за N минут до основного сигнала. */
    fun sunriseAt(alarm: GautoAlarm, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? {
        val trigger = nextTrigger(alarm, now) ?: return null
        return trigger.minusMinutes(alarm.sunriseMinutes.coerceIn(0, 30).toLong())
    }

    /** Нарастание громкости: начинаем тихо, приходим к целевой за rampSec. */
    fun escalationVolume(elapsedSec: Int, rampSec: Int, target: Int): Int {
        val ramp = rampSec.coerceIn(AlarmRules.MIN_RAMP_SEC, AlarmRules.MAX_RAMP_SEC)
        val t = (elapsedSec.coerceAtLeast(0).toFloat() / ramp).coerceIn(0f, 1f)
        val start = (target * 0.18f).roundToInt().coerceAtLeast(1)
        return (start + (target - start) * t).roundToInt().coerceIn(1, 100)
    }

    /** Авто-стоп — это завершение сигнала, а не откладывание. */
    fun autoStopReached(elapsedSec: Int, minutes: Int): Boolean =
        elapsedSec >= minutes.coerceIn(1, 60) * 60

    fun formatTimer(seconds: Int): String {
        val s = seconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    }

    /** Четвёртая ось: фаза дня 0..1 определяет глубину, цвет и вращение 4D-сцены. */
    fun phaseOfDay(time: LocalTime = LocalTime.now()): Float =
        (time.hour * 60 + time.minute) / 1440f

    fun sceneTitles(phase: Float): String = when {
        phase < 0.20f -> "глубокая ночь"
        phase < 0.30f -> "рассвет"
        phase < 0.55f -> "утро"
        phase < 0.75f -> "день"
        phase < 0.88f -> "закат"
        else -> "ночь"
    }

    fun validateAlarm(alarm: GautoAlarm): GautoAlarm = alarm.copy(
        hour = alarm.hour.coerceIn(0, 23),
        minute = alarm.minute.coerceIn(0, 59),
        days = alarm.days.filter { it in 1..7 }.distinct().sorted(),
        volume = alarm.volume.coerceIn(1, 100),
        rampSec = alarm.rampSec.coerceIn(AlarmRules.MIN_RAMP_SEC, AlarmRules.MAX_RAMP_SEC),
        sunriseMinutes = alarm.sunriseMinutes.coerceIn(0, 30),
        autoStopMinutes = alarm.autoStopMinutes.coerceIn(1, 60),
        wakeBrightness = alarm.wakeBrightness.coerceIn(1, 100),
        strict = true,
        sound = if (alarm.sound in AlarmRules.SOUNDS) alarm.sound else AlarmRules.SOUND_SUNRISE
    )

    fun validateTimer(timer: GautoTimer): GautoTimer = timer.copy(
        durationSec = timer.durationSec.coerceIn(5, 24 * 3600),
        volume = timer.volume.coerceIn(1, 100)
    )

    fun validateReminder(reminder: GautoReminder): GautoReminder = reminder.copy(
        hour = reminder.hour.coerceIn(0, 23),
        minute = reminder.minute.coerceIn(0, 59),
        days = reminder.days.filter { it in 1..7 }.distinct().sorted()
    )
}
