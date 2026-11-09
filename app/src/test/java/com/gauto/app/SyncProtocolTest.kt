package com.gauto.app

import com.gauto.app.core.GautoRule
import com.gauto.app.core.GautoState
import com.gauto.app.core.SyncProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncProtocolTest {

    private val sample = GautoState(
        device = "Gauto phone",
        updatedAt = 1_000L,
        rules = listOf(GautoRule(id = "a", name = "домашние дела", hour = 10, minute = 0)),
        events = listOf("тест")
    )

    @Test
    fun stateRoundTripKeepsRules() {
        val text = SyncProtocol.encode(sample)
        val back = SyncProtocol.decode(text)
        assertNotNull(back)
        assertEquals("Gauto phone", back!!.device)
        assertEquals(1, back.rules.size)
        assertEquals("домашние дела", back.rules[0].name)
        assertEquals(SyncProtocol.SCHEMA, back.schema)
    }

    @Test
    fun garbageAndForeignSchemaAreRejected() {
        assertNull(SyncProtocol.decode("просто текст"))
        assertNull(SyncProtocol.decode("{}"))
    }

    @Test
    fun newerStateWins() {
        val older = sample.copy(updatedAt = 500L, device = "ПК")
        val newer = sample.copy(updatedAt = 2_000L, device = "ПК")
        assertEquals("ПК", SyncProtocol.merge(sample, newer).device)
        assertEquals("Gauto phone", SyncProtocol.merge(sample, older).device)
        assertEquals("Gauto phone", SyncProtocol.merge(sample, sample.copy(updatedAt = 1_000L)).device)
    }

    @Test
    fun discoveryParsesOnlyOwnMagic() {
        val here = SyncProtocol.here(port = 8765, device = "Gauto|phone")
        val parsed = SyncProtocol.parseHere(here)
        assertNotNull(parsed)
        assertEquals(8765, parsed!!.port)
        assertEquals("Gauto phone", parsed.device)
        assertNull(SyncProtocol.parseHere("чужой сервис|1234|1|dev"))
        assertNull(SyncProtocol.parseHere("GAUTO_HERE_v1|abc|1|dev"))
        assertNull(SyncProtocol.parseHere("GAUTO_HERE_v1|8765|99|dev"))
        assertNull(SyncProtocol.parseHere("GAUTO_HERE_v1|99999|1|dev"))
    }

    @Test
    fun pinProtectsLanAccess() {
        assertTrue(SyncProtocol.pinValid("1234", "1234"))
        assertTrue(SyncProtocol.pinValid(" 1234 ", "1234"))
        assertFalse(SyncProtocol.pinValid("0000", "1234"))
        assertFalse(SyncProtocol.pinValid(null, "1234"))
        assertTrue(SyncProtocol.pinValid(null, ""))
        assertTrue(SyncProtocol.ping().contains("Gauto"))
    }
}
