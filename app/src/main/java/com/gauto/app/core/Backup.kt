package com.gauto.app.core

/** Резервная копия одним файлом: все настройки, правила и привычки в одном JSON. */
object Backup {
    const val FILE_NAME = "gauto-backup.json"

    fun exportText(store: GautoStore): String = SyncProtocol.encode(
        store.snapshot(now = System.currentTimeMillis())
    )

    fun importText(store: GautoStore, text: String): Boolean {
        val state = SyncProtocol.decode(text) ?: return false
        if (state.schema != SyncProtocol.SCHEMA) return false
        store.saveRules(state.rules)
        store.saveHabits(state.habits)
        store.saveSessions(state.sessions)
        store.saveAlarms(state.alarms)
        store.saveTimers(state.timers)
        store.saveReminders(state.reminders)
        store.saveEvents(state.events)
        store.saveSettings(state.settings.copy(syncEnabled = store.settings().syncEnabled, syncPin = store.settings().syncPin))
        store.event("Резервная копия восстановлена")
        return true
    }
}
