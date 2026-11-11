package com.gauto.app.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Единые константы GAUTO. Никакой зависимости от GSYNK и его репозитория обновлений. */
object GautoRepo {
    const val OWNER = "GrapeGG-developer"
    const val NAME = "GAUTO"
    const val REPO = "GrapeGG-developer/GAUTO"
    const val VERSION = "4.0.0"
    const val VERSION_CODE = 4
    const val UI_VERSION = "Gauto 4D Living Island 4.0"
    const val DEFAULT_BLOCKED = "com.google.android.youtube,com.google.android.apps.youtube.music,com.ss.android.ugc.trill,com.instagram.android"
    fun apkName(tag: String): String = "GAUTO-$tag-debug.apk"
    fun defaultBlocked(): List<String> = DEFAULT_BLOCKED.split(",")
}

@Serializable
data class GautoRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Новое правило",
    val hour: Int = 7,
    val minute: Int = 30,
    val days: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val enabled: Boolean = true,
    val useBrightness: Boolean = true,
    val brightness: Int = 40,
    val useVolume: Boolean = true,
    val volume: Int = 30,
    /** -1 — не трогать «Не беспокоить», 0 — выключить, 1 — включить. */
    val silentMode: Int = -1,
    /** Монохромный слой перед сном: сначала системный далтонизатор, затем оверлей. */
    val grayscale: Boolean = false,
    /** Пакеты приложений, которые закрывает активное правило. */
    val blockPackages: List<String> = emptyList(),
    val habitId: String = "",
    val packId: String = ""
) {
    val timeLabel: String get() = "%02d:%02d".format(hour, minute)
    val minuteOfDay: Int get() = hour * 60 + minute
    fun effectiveDays(): Set<Int> = days.filter { it in 1..7 }.toSet()
    fun daysLabel(): String = when {
        days.toSet() == (1..7).toSet() -> "Каждый день"
        days.toSet() == setOf(1, 2, 3, 4, 5) -> "По будням"
        days.toSet() == setOf(6, 7) -> "По выходным"
        days.isEmpty() -> "Дни не выбраны"
        else -> days.sorted().joinToString(" ") { RuleLogic.DAY_SHORT[it - 1] }
    }
}

@Serializable
data class GautoSettings(
    val autopilot: Boolean = true,
    val notifications: Boolean = true,
    val syncAfterBoot: Boolean = true,
    val ruleSort: Int = RuleSort.BY_TIME,
    val updateRepo: String = GautoRepo.REPO,
    val updateAuto: Boolean = true,
    val etag: String = "",
    val lastUpdateCheck: Long = 0L,
    val seenVersion: String = "",
    val accent: Int = 0,
    val darkTheme: Boolean = true,
    val haptics: Boolean = true,
    val focusMinutes: Int = 25,
    val xp: Int = 0,
    val streak: Int = 0,
    val lastHabitDate: String = "",
    val autoBrightnessSaved: Boolean = false,
    val autoBrightnessWasOn: Boolean = false,
    /** Синхронизация с ПК по локальной сети. */
    val syncEnabled: Boolean = false,
    /** Короткий PIN для LAN-запросов; пустой = без проверки (только доверенная сеть). */
    val syncPin: String = "",
    val syncDeviceName: String = "Gauto phone",
    val lastSyncAt: Long = 0L,
    /** Живость анимаций: 0 — спокойно, 1 — обычно, 2 — максимум. */
    val motionLevel: Int = 2,
    /** 4D-сцена: глубина + время как четвёртая ось. */
    val fourDEnabled: Boolean = true,
    /** Идентификатор активного будильника: пока не подтверждён, блокировки действуют. */
    val activeAlarmId: String = ""
)

@Serializable
data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Привычка",
    val subtitle: String = "",
    val icon: String = "check",
    val xp: Int = 10,
    val targetPerWeek: Int = 7,
    val doneDates: List<String> = emptyList(),
    val ruleId: String = ""
)

@Serializable
data class FocusSession(
    val id: String = UUID.randomUUID().toString(),
    val startedAt: Long = 0L,
    val minutes: Int = 25,
    val completed: Boolean = false,
    val blockedPackages: List<String> = GautoRepo.defaultBlocked(),
    val label: String = "глубокий труд"
)

data class PackSpec(
    val suffix: String,
    val name: String,
    val hour: Int,
    val minute: Int,
    val days: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val brightness: Int = 50,
    val volume: Int = 30,
    val silentMode: Int = -1,
    val grayscale: Boolean = false,
    val blockPackages: List<String> = emptyList(),
    val habitId: String = ""
)

data class LifePack(
    val id: String,
    val title: String,
    val hint: String,
    val icon: String,
    val specs: List<PackSpec>
)

object RuleSort {
    const val BY_TIME = 0
    const val BY_NAME = 1
    const val ACTIVE_FIRST = 2
    val ALL = listOf(BY_TIME, BY_NAME, ACTIVE_FIRST)
    fun label(value: Int): String = when (value) {
        BY_NAME -> "По названию"
        ACTIVE_FIRST -> "Активные сверху"
        else -> "По времени"
    }
    fun apply(rules: List<GautoRule>, mode: Int): List<GautoRule> = when (mode) {
        BY_NAME -> rules.sortedBy { it.name.lowercase() }
        ACTIVE_FIRST -> rules.sortedWith(compareByDescending<GautoRule> { it.enabled }.thenBy { it.minuteOfDay })
        else -> rules.sortedBy { it.minuteOfDay }
    }
}

/** Чистая математика правил: без Android-зависимостей, полностью покрыта тестами. */
object RuleLogic {
    val DAY_SHORT = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
    val DAY_FULL = listOf(
        "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"
    )

    fun percentToVolumeIndex(percent: Int, maxIndex: Int): Int {
        if (maxIndex <= 0) return 0
        return (percent.coerceIn(0, 100) / 100f * maxIndex).roundToInt().coerceIn(0, maxIndex)
    }

    fun volumeIndexesToPercent(index: Int, maxIndex: Int): Int {
        if (maxIndex <= 0) return 0
        return (index * 100f / maxIndex).roundToInt().coerceIn(0, 100)
    }

    fun percentToBrightnessRaw(percent: Int): Int =
        (percent.coerceIn(0, 100) / 100f * 255f).roundToInt().coerceIn(1, 255)

    fun validate(rule: GautoRule): GautoRule = rule.copy(
        hour = rule.hour.coerceIn(0, 23),
        minute = rule.minute.coerceIn(0, 59),
        brightness = rule.brightness.coerceIn(0, 100),
        volume = rule.volume.coerceIn(0, 100),
        days = rule.days.filter { it in 1..7 }.distinct().sorted(),
        silentMode = rule.silentMode.coerceIn(-1, 1),
        blockPackages = rule.blockPackages.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    )

    fun nextOccurrence(rule: GautoRule, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? {
        if (!rule.enabled) return null
        val days = rule.effectiveDays()
        if (days.isEmpty()) return null
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (days.contains(date.dayOfWeek.value)) {
                val candidate = date.atTime(rule.hour, rule.minute).atZone(now.zone)
                if (candidate.isAfter(now)) return candidate
            }
        }
        return null
    }

    fun lastOccurrence(rule: GautoRule, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? {
        if (!rule.enabled) return null
        val days = rule.effectiveDays()
        if (days.isEmpty()) return null
        for (offset in 0..7) {
            val date = now.toLocalDate().minusDays(offset.toLong())
            if (days.contains(date.dayOfWeek.value)) {
                val candidate = date.atTime(rule.hour, rule.minute).atZone(now.zone)
                if (!candidate.isAfter(now)) return candidate
            }
        }
        return null
    }

    /**
     * Активное правило = последнее по времени ≤ сейчас. Именно оно применяется после
     * перезагрузки и определяет, какие приложения заблокированы прямо сейчас.
     */
    fun currentRule(rules: List<GautoRule>, now: ZonedDateTime = ZonedDateTime.now()): GautoRule? {
        val enabled = rules.filter { it.enabled }
        if (enabled.isEmpty()) return null
        val minute = now.hour * 60 + now.minute
        val today = enabled.filter { now.dayOfWeek.value in it.effectiveDays() }
        val pastToday = today.filter { it.minuteOfDay <= minute }.maxByOrNull { it.minuteOfDay }
        if (pastToday != null) return pastToday
        for (offset in 1..7) {
            val day = now.toLocalDate().minusDays(offset.toLong()).dayOfWeek.value
            val candidate = enabled.filter { day in it.effectiveDays() }.maxByOrNull { it.minuteOfDay }
            if (candidate != null) return candidate
        }
        return enabled.maxByOrNull { it.minuteOfDay }
    }

    fun matches(query: String, rule: GautoRule): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return rule.name.lowercase().contains(q) ||
            rule.timeLabel.contains(q) ||
            rule.daysLabel().lowercase().contains(q) ||
            rule.blockPackages.any { it.lowercase().contains(q) }
    }
}

/** Готовые наборы: 16 сценариев × 4 правила = 64 готовых автоматизации. */
object LifePacks {
    private val weekdays = listOf(1, 2, 3, 4, 5)
    private val weekend = listOf(6, 7)
    private val everyDay = (1..7).toList()

    private fun spec(
        suffix: String, name: String, hour: Int, minute: Int,
        days: List<Int> = everyDay, brightness: Int = 50, volume: Int = 30,
        silentMode: Int = -1, grayscale: Boolean = false,
        blockPackages: List<String> = emptyList(), habitId: String = ""
    ) = PackSpec(suffix, name, hour, minute, days, brightness, volume, silentMode, grayscale, blockPackages, habitId)

    private fun pack(id: String, title: String, hint: String, icon: String, vararg specs: PackSpec) =
        LifePack(id, title, hint, icon, specs.toList())

    val ALL: List<LifePack> = listOf(
        pack("school-home", "школа и дом", "тихо утром, ярко после занятий", "school",
            spec("morning", "школа · тихо", 7, 0, weekdays, 28, 0, 1),
            spec("lesson", "уроки · фокус", 10, 0, weekdays, 45, 0, 1, blockPackages = GautoRepo.defaultBlocked()),
            spec("home", "дом · youtube", 15, 0, weekdays, 85, 80, 0),
            spec("evening", "вечер · спокойно", 20, 0, everyDay, 35, 25, -1)),
        pack("chores", "домашние дела", "YouTube закрыт, пока не убрано", "cleaning",
            spec("start", "уборка · старт", 10, 0, everyDay, 70, 20, -1, blockPackages = GautoRepo.defaultBlocked(), habitId = "chores"),
            spec("deep", "уборка · без отвлечений", 11, 30, everyDay, 75, 15, 1, blockPackages = GautoRepo.defaultBlocked(), habitId = "chores"),
            spec("finish", "уборка · финиш", 13, 0, everyDay, 70, 35, 0, habitId = "chores"),
            spec("rest", "после дел · отдых", 13, 30, everyDay, 65, 55, 0)),
        pack("sleep", "сон без залипания", "экран монохромный, уведомления тихие", "bedtime",
            spec("wind", "подготовка ко сну", 21, 30, everyDay, 30, 10, 1, grayscale = true, blockPackages = GautoRepo.defaultBlocked()),
            spec("night", "ночь · ч/б", 23, 0, everyDay, 12, 0, 1, grayscale = true, blockPackages = GautoRepo.defaultBlocked(), habitId = "sleep"),
            spec("wake", "подъём", 7, 0, everyDay, 65, 45, 0),
            spec("day", "день · обычный", 8, 0, everyDay, 70, 50, 0)),
        pack("deep-code", "глубокий код", "два блока без соцсетей", "code",
            spec("block1", "код · блок 1", 9, 0, weekdays, 60, 0, 1, blockPackages = GautoRepo.defaultBlocked(), habitId = "code"),
            spec("break1", "перерыв", 10, 30, weekdays, 65, 35, 0),
            spec("block2", "код · блок 2", 11, 0, weekdays, 60, 0, 1, blockPackages = GautoRepo.defaultBlocked(), habitId = "code"),
            spec("review", "ревью и записи", 14, 0, weekdays, 60, 25, 0, habitId = "code")),
        pack("focus-uni", "учёба и экзамены", "короткие спурты и тишина", "study",
            spec("morning", "повторение", 8, 0, weekdays, 60, 0, 1, blockPackages = GautoRepo.defaultBlocked()),
            spec("sprint", "спурт 90 минут", 11, 0, weekdays, 55, 0, 1, blockPackages = GautoRepo.defaultBlocked()),
            spec("break", "отдых", 12, 30, weekdays, 65, 40, 0),
            spec("exam", "перед экзаменом", 18, 0, everyDay, 50, 0, 1, blockPackages = GautoRepo.defaultBlocked())),
        pack("morning-dev", "утро разработчика", "план дня до первых отвлечений", "sun",
            spec("wake", "подъём", 6, 30, weekdays, 60, 30, 0),
            spec("plan", "план и стендап", 7, 0, weekdays, 65, 0, 1, blockPackages = GautoRepo.defaultBlocked(), habitId = "plan"),
            spec("first-commit", "первый коммит", 8, 30, weekdays, 65, 0, 1, blockPackages = GautoRepo.defaultBlocked(), habitId = "code"),
            spec("coffee", "кофе-пауза", 10, 0, weekdays, 70, 40, 0)),
        pack("weekend", "выходные без стопа", "дела утром, отдых после", "weekend",
            spec("morning", "суббота · дела", 9, 0, weekend, 75, 30, 0, blockPackages = GautoRepo.defaultBlocked(), habitId = "chores"),
            spec("noon", "полдень · свободно", 12, 0, weekend, 80, 60, 0),
            spec("walk", "прогулка", 16, 0, weekend, 85, 45, 0),
            spec("night", "воскресенье · сон", 22, 0, weekend, 25, 0, 1, grayscale = true)),
        pack("eyes", "глаза и батарея", "щадящая яркость вечером", "eye",
            spec("day", "день", 9, 0, everyDay, 70, 45, 0),
            spec("dusk", "сумерки", 18, 0, everyDay, 45, 30, 0),
            spec("night", "ночь", 22, 0, everyDay, 18, 0, 1, grayscale = true),
            spec("charge", "зарядка", 23, 30, everyDay, 10, 0, 1, grayscale = true)),
        pack("sport", "спорт", "громко на тренировке, тихо после", "sport",
            spec("warmup", "разминка", 7, 0, everyDay, 75, 70, 0),
            spec("train", "тренировка", 18, 0, everyDay, 80, 85, 0, habitId = "sport"),
            spec("shower", "душ", 19, 30, everyDay, 55, 40, 0),
            spec("recovery", "восстановление", 21, 0, everyDay, 30, 10, 1, grayscale = true)),
        pack("reading", "чтение", "бумажный режим без ленты", "book",
            spec("start", "чтение · старт", 20, 0, everyDay, 40, 0, 1, blockPackages = GautoRepo.defaultBlocked(), habitId = "read"),
            spec("deep", "чтение · тишина", 20, 30, everyDay, 35, 0, 1, grayscale = true, blockPackages = GautoRepo.defaultBlocked()),
            spec("notes", "заметки", 21, 15, everyDay, 45, 0, 0),
            spec("sleep", "сон", 22, 30, everyDay, 15, 0, 1, grayscale = true)),
        pack("transport", "дорога", "экономия батареи и тишина", "transport",
            spec("out", "выход", 7, 30, weekdays, 45, 20, 1),
            spec("road", "в пути", 8, 0, weekdays, 35, 15, 1),
            spec("arrive", "на месте", 9, 0, weekdays, 65, 40, 0),
            spec("back", "обратно", 18, 0, weekdays, 40, 20, 1)),
        pack("cinema", "кино", "ярко и громко только во время фильма", "movie",
            spec("prepare", "подготовка", 19, 30, everyDay, 35, 20, 1),
            spec("film", "фильм", 20, 0, everyDay, 100, 90, 1),
            spec("after", "после фильма", 22, 0, everyDay, 30, 15, 1, grayscale = true),
            spec("sleep", "сон", 23, 0, everyDay, 12, 0, 1, grayscale = true)),
        pack("gaming", "игры без перебора", "ограничить вечерний залип", "game",
            spec("start", "игра · старт", 19, 0, weekend, 85, 75, 0),
            spec("limit", "игра · лимит", 21, 0, weekend, 60, 45, 0),
            spec("stop", "игра · стоп", 22, 0, weekend, 25, 10, 1, grayscale = true, blockPackages = GautoRepo.defaultBlocked()),
            spec("sleep", "сон", 23, 0, weekend, 12, 0, 1, grayscale = true)),
        pack("pomodoro", "pomodoro", "чередование работы и пауз", "timer",
            spec("work1", "работа 25", 9, 0, weekdays, 60, 0, 1, blockPackages = GautoRepo.defaultBlocked()),
            spec("break1", "пауза 5", 9, 25, weekdays, 65, 30, 0),
            spec("work2", "работа 25", 9, 30, weekdays, 60, 0, 1, blockPackages = GautoRepo.defaultBlocked()),
            spec("long", "большая пауза", 10, 0, weekdays, 70, 45, 0)),
        pack("digital-detox", "детокс", "день без коротких видео", "detox",
            spec("morning", "утро без ленты", 8, 0, weekend, 65, 25, 1, blockPackages = GautoRepo.defaultBlocked(), habitId = "detox"),
            spec("day", "день без ленты", 12, 0, weekend, 75, 35, 0, blockPackages = GautoRepo.defaultBlocked()),
            spec("evening", "вечер без ленты", 18, 0, weekend, 45, 20, 1, grayscale = true, blockPackages = GautoRepo.defaultBlocked()),
            spec("night", "сон", 22, 0, weekend, 15, 0, 1, grayscale = true)),
        pack("release-day", "день релиза", "собрать, проверить, не отвлекаться", "rocket",
            spec("build", "сборка", 10, 0, weekdays, 65, 0, 1, blockPackages = GautoRepo.defaultBlocked(), habitId = "code"),
            spec("check", "проверка", 12, 0, weekdays, 65, 0, 1, blockPackages = GautoRepo.defaultBlocked()),
            spec("publish", "публикация", 15, 0, weekdays, 70, 30, 0),
            spec("rest", "отдых после релиза", 19, 0, weekdays, 45, 40, 0))
    )

    fun byId(id: String): LifePack? = ALL.firstOrNull { it.id == id }

    fun rules(pack: LifePack): List<GautoRule> = pack.specs.map { s ->
        RuleLogic.validate(
            GautoRule(
                id = "life-${pack.id}-${s.suffix}",
                name = s.name,
                hour = s.hour,
                minute = s.minute,
                days = s.days,
                brightness = s.brightness,
                volume = s.volume,
                silentMode = s.silentMode,
                grayscale = s.grayscale,
                blockPackages = s.blockPackages,
                habitId = s.habitId,
                packId = pack.id
            )
        )
    }

    fun install(existing: List<GautoRule>, pack: LifePack): List<GautoRule> {
        val generated = rules(pack).associateBy { it.id }
        val merged = existing.associateBy { it.id }.toMutableMap()
        generated.forEach { (id, rule) -> merged[id] = rule }
        return merged.values.toList().sortedBy { it.minuteOfDay }
    }

    fun isInstalled(rules: List<GautoRule>, pack: LifePack): Boolean {
        val ids = rules(pack).map { it.id }.toSet()
        return rules.any { it.id in ids }
    }

    fun defaultHabits(): List<Habit> = listOf(
        Habit(id = "chores", title = "домашние дела", subtitle = "убрано до обеда", icon = "cleaning", xp = 15, targetPerWeek = 5),
        Habit(id = "sleep", title = "лёг до 23:00", subtitle = "экран ч/б, без залипания", icon = "bedtime", xp = 20, targetPerWeek = 7),
        Habit(id = "code", title = "глубокий код", subtitle = "блок без соцсетей", icon = "code", xp = 25, targetPerWeek = 5),
        Habit(id = "read", title = "чтение", subtitle = "20 страниц без телефона", icon = "book", xp = 12, targetPerWeek = 5),
        Habit(id = "sport", title = "движение", subtitle = "тренировка или прогулка", icon = "sport", xp = 18, targetPerWeek = 4),
        Habit(id = "plan", title = "план дня", subtitle = "записано до первого коммита", icon = "sun", xp = 10, targetPerWeek = 5),
        Habit(id = "detox", title = "без коротких видео", subtitle = "день без ленты", icon = "detox", xp = 22, targetPerWeek = 2)
    )
}

/** Геймификация: стрики, очки и «уровень разработчика». */
object HabitsLogic {
    fun streak(doneDates: List<String>, today: LocalDate = LocalDate.now()): Int {
        val set = doneDates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
        var cursor = today
        var count = 0
        while (set.contains(cursor)) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    fun weekProgress(habit: Habit, today: LocalDate = LocalDate.now()): Int {
        val start = today.minusDays(6L)
        return habit.doneDates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .count { !it.isBefore(start) && !it.isAfter(today) }
            .coerceIn(0, habit.targetPerWeek)
    }

    fun level(xp: Int): Int = 1 + sqrt((xp.coerceAtLeast(0) / 50.0).toFloat()).toInt()

    fun xpForLevel(level: Int): Int = (level - 1) * (level - 1) * 50

    fun progress(xp: Int): Float {
        val lvl = level(xp)
        val from = xpForLevel(lvl)
        val to = xpForLevel(lvl + 1)
        if (to <= from) return 1f
        return ((xp - from).toFloat() / (to - from)).coerceIn(0f, 1f)
    }

    fun xpFor(habit: Habit, consecutive: Int): Int = habit.xp + (consecutive * 2).coerceAtMost(20)

    fun title(xp: Int): String = when (level(xp)) {
        1 -> "новичок"
        2 -> "стажёр"
        3 -> "младший разработчик"
        4 -> "разработчик"
        5 -> "уверенный разработчик"
        else -> "ведущий разработчик"
    }
}

object FocusLogic {
    fun remainingSeconds(session: FocusSession, now: Long = System.currentTimeMillis()): Long =
        (((session.startedAt + session.minutes * 60_000L - now) / 1000L)).coerceAtLeast(0L)

    fun isActive(session: FocusSession, now: Long = System.currentTimeMillis()): Boolean =
        !session.completed && remainingSeconds(session, now) > 0L

    fun xp(minutes: Int): Int = (minutes * 2).coerceAtLeast(1)

    fun label(seconds: Long): String = "%02d:%02d".format(seconds / 60, seconds % 60)
}

/** DTO и чистый разбор ответа GitHub: без токена, только публичный API. */
@Serializable
data class ReleaseDto(
    val tag_name: String = "",
    val name: String = "",
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<AssetDto> = emptyList()
)

@Serializable
data class AssetDto(
    val name: String = "",
    val size: Long = 0L,
    val browser_download_url: String = "",
    val digest: String = ""
)

data class GautoUpdate(
    val tag: String,
    val name: String,
    val apkUrl: String,
    val size: Long,
    val sha256: String
)

object UpdateLogic {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseLatest(text: String): GautoUpdate? = runCatching {
        val dto = json.decodeFromString(ReleaseDto.serializer(), text)
        if (dto.tag_name.isBlank() || dto.draft || dto.prerelease) return@runCatching null
        val asset = dto.assets.firstOrNull { it.name.endsWith(".apk") } ?: return@runCatching null
        val url = asset.browser_download_url.ifBlank { apkUrl(GautoRepo.REPO, dto.tag_name) }
        GautoUpdate(
            tag = dto.tag_name,
            name = dto.name.ifBlank { dto.tag_name },
            apkUrl = url,
            size = asset.size,
            sha256 = shaFromDigest(asset.digest)
        )
    }.getOrNull()

    fun shaFromDigest(digest: String?): String =
        digest.orEmpty().substringAfter("sha256:", "").trim()

    fun apkUrl(repo: String, tag: String): String =
        "https://github.com/$repo/releases/download/$tag/${GautoRepo.apkName(tag)}"

    fun versionCodeFromTag(tag: String): Int {
        val parts = tag.trim().removePrefix("v").split(".", "-")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return major * 10_000 + minor * 100 + patch
    }

    fun isNewer(
        tag: String,
        currentCode: Int = versionCodeFromTag("v" + GautoRepo.VERSION)
    ): Boolean = versionCodeFromTag(tag) > currentCode

    fun safeApkFileName(tag: String): String = GautoRepo.apkName(tag)
}
