package com.gauto.app.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Протокол синхронизации Gauto «телефон ⇄ ПК» по локальной сети.
 *
 * Никакого облака, аккаунта и токена: телефон поднимает HTTP-сервер на [PORT],
 * ПК находит его UDP-запросом на [DISCOVERY_PORT] и обменивается одним JSON.
 * Доступ защищён коротким PIN из настроек (заголовок [PIN_HEADER]).
 */
object SyncProtocol {
    const val SCHEMA = 1
    const val PORT = 8765
    const val DISCOVERY_PORT = 8766
    const val DISCOVER_MAGIC = "GAUTO_DISCOVER_v1"
    const val HERE_MAGIC = "GAUTO_HERE_v1"
    const val PIN_HEADER = "X-Gauto-Pin"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun here(port: Int = PORT, device: String, schema: Int = SCHEMA): String =
        "$HERE_MAGIC|$port|$schema|${device.replace('|', ' ').take(48)}"

    data class Here(val port: Int, val schema: Int, val device: String)

    fun parseHere(message: String): Here? {
        val parts = message.trim().split("|")
        if (parts.size < 4 || parts[0] != HERE_MAGIC) return null
        val port = parts[1].toIntOrNull() ?: return null
        val schema = parts[2].toIntOrNull() ?: return null
        if (port !in 1..65535 || schema != SCHEMA) return null
        return Here(port, schema, parts[3])
    }

    fun encode(state: GautoState): String = json.encodeToString(GautoState.serializer(), state)

    fun decode(text: String): GautoState? = runCatching {
        val state = json.decodeFromString(GautoState.serializer(), text)
        if (state.schema != SCHEMA || !state.app.equals("Gauto", ignoreCase = true) || state.device.isBlank()) null
        else state
    }.getOrNull()

    /** Побеждает более свежее состояние; при равенстве сохраняем локальное. */
    fun merge(local: GautoState, remote: GautoState): GautoState =
        if (remote.updatedAt > local.updatedAt) remote else local

    fun pinValid(provided: String?, pin: String): Boolean {
        if (pin.isBlank()) return true
        return !provided.isNullOrBlank() && provided.trim() == pin.trim()
    }

    fun ping(): String = "{\"ok\":true,\"app\":\"Gauto\",\"schema\":$SCHEMA}"
}

@Serializable
data class GautoState(
    val schema: Int = SyncProtocol.SCHEMA,
    val app: String = "Gauto",
    val device: String = "",
    val updatedAt: Long = 0L,
    val rules: List<GautoRule> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val sessions: List<FocusSession> = emptyList(),
    val settings: GautoSettings = GautoSettings(),
    val events: List<String> = emptyList()
)
