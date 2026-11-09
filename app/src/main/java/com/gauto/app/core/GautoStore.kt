package com.gauto.app.core

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Локальное хранилище GAUTO. Никакой сети, никакой зависимости от GSYNK:
 * правила, привычки, фокус-сессии, очки и настройки живут только на телефоне.
 */
class GautoStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("gauto_store", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun rules(): List<GautoRule> {
        val raw = prefs.getString(KEY_RULES, null) ?: return emptyList()
        return runCatching { json.decodeFromString(ListSerializer(GautoRule.serializer()), raw) }
            .getOrDefault(emptyList())
    }

    fun saveRules(list: List<GautoRule>) {
        val clean = list.map { RuleLogic.validate(it) }.sortedBy { it.minuteOfDay }
        prefs.edit().putString(KEY_RULES, json.encodeToString(ListSerializer(GautoRule.serializer()), clean)).apply()
    }

    fun upsertRule(rule: GautoRule) {
        val list = rules().toMutableList()
        val index = list.indexOfFirst { it.id == rule.id }
        if (index >= 0) list[index] = RuleLogic.validate(rule) else list.add(RuleLogic.validate(rule))
        saveRules(list)
    }

    fun deleteRule(id: String) = saveRules(rules().filterNot { it.id == id })

    fun setRuleEnabled(id: String, enabled: Boolean) {
        saveRules(rules().map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    fun installPack(packId: String): Int {
        val pack = LifePacks.byId(packId) ?: return 0
        val before = rules().size
        saveRules(LifePacks.install(rules(), pack))
        val habits = habits().toMutableList()
        val needed = LifePacks.rules(pack).map { it.habitId }.filter { it.isNotBlank() }.distinct()
        needed.forEach { hid ->
            if (habits.none { it.id == hid }) {
                LifePacks.defaultHabits().firstOrNull { it.id == hid }?.let { habits.add(it) }
            }
        }
        saveHabits(habits)
        return rules().size - before
    }

    fun habits(): List<Habit> {
        val raw = prefs.getString(KEY_HABITS, null) ?: return LifePacks.defaultHabits()
        return runCatching { json.decodeFromString(ListSerializer(Habit.serializer()), raw) }
            .getOrDefault(LifePacks.defaultHabits())
    }

    fun saveHabits(list: List<Habit>) {
        prefs.edit().putString(KEY_HABITS, json.encodeToString(ListSerializer(Habit.serializer()), list)).apply()
    }

    fun markHabitDone(id: String, date: LocalDate = LocalDate.now()) {
        val iso = date.toString()
        val updated = habits().map { habit ->
            if (habit.id == id && iso !in habit.doneDates) {
                val next = (habit.doneDates + iso).distinct().sorted()
                habit.copy(doneDates = next)
            } else habit
        }
        saveHabits(updated)
        val habit = updated.firstOrNull { it.id == id } ?: return
        val consecutive = HabitsLogic.streak(habit.doneDates, date)
        addXp(HabitsLogic.xpFor(habit, consecutive))
        saveSettings(settings().copy(streak = HabitsLogic.streak(habit.doneDates, date), lastHabitDate = iso))
        event("«${habit.title}» выполнено · +${HabitsLogic.xpFor(habit, consecutive)} xp")
    }

    fun sessions(): List<FocusSession> {
        val raw = prefs.getString(KEY_SESSIONS, null) ?: return emptyList()
        return runCatching { json.decodeFromString(ListSerializer(FocusSession.serializer()), raw) }
            .getOrDefault(emptyList())
    }

    fun addSession(session: FocusSession) {
        val list = (sessions() + session).takeLast(60)
        prefs.edit().putString(KEY_SESSIONS, json.encodeToString(ListSerializer(FocusSession.serializer()), list)).apply()
        event("Фокус «${session.label}» · ${session.minutes} мин")
    }

    fun saveSessions(list: List<FocusSession>) {
        prefs.edit().putString(KEY_SESSIONS, json.encodeToString(ListSerializer(FocusSession.serializer()), list.takeLast(60))).apply()
    }

    fun saveEvents(list: List<String>) {
        prefs.edit().putString(KEY_EVENTS, list.takeLast(30).joinToString("\n") { it.replace('\n', ' ') }).apply()
    }

    fun ensurePin(): String {
        val current = settings().syncPin
        if (current.length >= 4) return current
        val pin = (1000..9999).random().toString()
        saveSettings(settings().copy(syncPin = pin))
        return pin
    }

    fun snapshot(device: String = settings().syncDeviceName, now: Long = settings().lastSyncAt): GautoState =
        GautoState(
            schema = SyncProtocol.SCHEMA,
            app = "Gauto",
            device = device,
            updatedAt = now,
            rules = rules(),
            habits = habits(),
            sessions = sessions(),
            settings = settings(),
            events = events()
        )

    /** Применяет состояние от ПК, если оно свежее; возвращает принятое состояние. */
    fun applyRemote(text: String): GautoState? {
        val remote = SyncProtocol.decode(text) ?: return null
        if (remote.schema != SyncProtocol.SCHEMA) return null
        val local = snapshot(remote.device)
        val winner = SyncProtocol.merge(local, remote)
        if (winner === local || winner.updatedAt <= local.updatedAt) return local
        saveRules(winner.rules)
        saveHabits(winner.habits)
        saveSessions(winner.sessions)
        saveEvents(winner.events)
        saveSettings(
            winner.settings.copy(
                syncEnabled = settings().syncEnabled,
                syncPin = settings().syncPin,
                lastSyncAt = winner.updatedAt
            )
        )
        event("Синхронизировано с ПК · ${winner.device}")
        return winner
    }

    fun completeSession(id: String) {
        val list = sessions().map { if (it.id == id) it.copy(completed = true) else it }
        prefs.edit().putString(KEY_SESSIONS, json.encodeToString(ListSerializer(FocusSession.serializer()), list)).apply()
        val session = list.firstOrNull { it.id == id } ?: return
        addXp(FocusLogic.xp(session.minutes))
        event("Фокус завершён · +${FocusLogic.xp(session.minutes)} xp")
    }

    fun settings(): GautoSettings {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return GautoSettings()
        return runCatching { json.decodeFromString(GautoSettings.serializer(), raw) }
            .getOrDefault(GautoSettings())
    }

    fun saveSettings(value: GautoSettings) {
        prefs.edit().putString(KEY_SETTINGS, json.encodeToString(GautoSettings.serializer(), value)).apply()
    }

    fun addXp(delta: Int) {
        val s = settings()
        saveSettings(s.copy(xp = (s.xp + delta).coerceAtLeast(0)))
    }

    fun events(): List<String> =
        prefs.getString(KEY_EVENTS, "").orEmpty().split("\n").filter { it.isNotBlank() }

    fun event(text: String) {
        val list = (events() + text.replace('\n', ' ')).takeLast(30)
        prefs.edit().putString(KEY_EVENTS, list.joinToString("\n")).apply()
    }

    fun updateState(etag: String, lastCheck: Long, seenVersion: String) {
        saveSettings(settings().copy(etag = etag, lastUpdateCheck = lastCheck, seenVersion = seenVersion))
    }

    fun activeRule(now: ZonedDateTime = ZonedDateTime.now()): GautoRule? =
        RuleLogic.currentRule(rules(), now)

    fun activeBlockingLabel(now: Long = System.currentTimeMillis()): String {
        val active = activeRule()
        if (active != null && active.blockPackages.isNotEmpty()) return active.name
        val focus = sessions().lastOrNull { FocusLogic.isActive(it, now) }
        return focus?.label ?: "правило GAUTO"
    }

    /** Блокировка действует, пока активно правило или идёт фокус-сессия. */
    fun isPackageBlocked(pkg: String, now: Long = System.currentTimeMillis()): Boolean {
        val active = activeRule()
        if (active != null && active.blockPackages.any { matches(pkg, it) }) return true
        val focus = sessions().lastOrNull { FocusLogic.isActive(it, now) }
        if (focus != null && focus.blockedPackages.any { matches(pkg, it) }) return true
        return false
    }

    private fun matches(actual: String, blocked: String): Boolean =
        actual == blocked || (blocked.isNotBlank() && actual.startsWith("$blocked."))

    companion object {
        private const val KEY_RULES = "rules"
        private const val KEY_HABITS = "habits"
        private const val KEY_SESSIONS = "sessions"
        private const val KEY_SETTINGS = "settings"
        private const val KEY_EVENTS = "events"
    }
}
