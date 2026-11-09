package com.gauto.app

import com.gauto.app.core.GautoRule
import com.gauto.app.core.RuleLogic
import com.gauto.app.core.RuleSort
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleLogicTest {

    private val zone = ZoneId.of("Europe/Moscow")
    private fun at(day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, zone)

    @Test
    fun percentMathIsBounded() {
        assertEquals(0, RuleLogic.percentToVolumeIndex(0, 15))
        assertEquals(7, RuleLogic.percentToVolumeIndex(45, 15))
        assertEquals(15, RuleLogic.percentToVolumeIndex(100, 15))
        assertEquals(15, RuleLogic.percentToVolumeIndex(150, 15))
        assertEquals(0, RuleLogic.percentToVolumeIndex(-20, 15))
        assertEquals(0, RuleLogic.percentToVolumeIndex(50, 0))
        assertEquals(64, RuleLogic.percentToBrightnessRaw(25))
        assertEquals(191, RuleLogic.percentToBrightnessRaw(75))
        assertEquals(255, RuleLogic.percentToBrightnessRaw(100))
        assertEquals(1, RuleLogic.percentToBrightnessRaw(0))
        assertEquals(255, RuleLogic.percentToBrightnessRaw(999))
        assertEquals(47, RuleLogic.volumeIndexesToPercent(7, 15))
    }

    @Test
    fun currentRuleIsLatestPastTimeSoBootRestoreIsStable() {
        val morning = GautoRule(id = "morning", hour = 7, minute = 0, name = "утро")
        val night = GautoRule(id = "night", hour = 22, minute = 0, name = "сон", grayscale = true)
        val rules = listOf(night, morning)
        assertEquals("morning", RuleLogic.currentRule(rules, at(12, 8, 0))?.id)
        assertEquals("night", RuleLogic.currentRule(rules, at(12, 23, 0))?.id)
        assertEquals("night", RuleLogic.currentRule(rules, at(12, 3, 0))?.id)
        assertNull(RuleLogic.currentRule(emptyList(), at(12, 8, 0)))
    }

    @Test
    fun disabledRulesNeverFire() {
        val disabled = GautoRule(enabled = false)
        assertNull(RuleLogic.nextOccurrence(disabled, at(12, 8, 0)))
        assertNull(RuleLogic.lastOccurrence(disabled, at(12, 8, 0)))
        val noDays = GautoRule(days = emptyList())
        assertNull(RuleLogic.nextOccurrence(noDays, at(12, 8, 0)))
        val enabled = GautoRule(hour = 9, minute = 30)
        assertTrue(RuleLogic.nextOccurrence(enabled, at(12, 8, 0)) != null)
        assertTrue(RuleLogic.lastOccurrence(enabled, at(12, 10, 0)) != null)
    }

    @Test
    fun validateClampsEveryField() {
        val dirty = GautoRule(
            hour = 99, minute = -5, brightness = 500, volume = -10,
            days = listOf(9, 1, 1, 7), silentMode = 8,
            blockPackages = listOf(" com.google.android.youtube ", "", "com.google.android.youtube")
        )
        val clean = RuleLogic.validate(dirty)
        assertEquals(23, clean.hour)
        assertEquals(0, clean.minute)
        assertEquals(100, clean.brightness)
        assertEquals(0, clean.volume)
        assertEquals(listOf(1, 7), clean.days)
        assertEquals(1, clean.silentMode)
        assertEquals(listOf("com.google.android.youtube"), clean.blockPackages)
        assertEquals("23:00", clean.timeLabel)
    }

    @Test
    fun labelsAndSortAndSearch() {
        assertEquals("Каждый день", GautoRule(days = (1..7).toList()).daysLabel())
        assertEquals("По будням", GautoRule(days = listOf(1, 2, 3, 4, 5)).daysLabel())
        assertEquals("По выходным", GautoRule(days = listOf(6, 7)).daysLabel())
        assertEquals("Дни не выбраны", GautoRule(days = emptyList()).daysLabel())
        val a = GautoRule(name = "Б", hour = 18, enabled = false)
        val b = GautoRule(name = "А", hour = 7, enabled = true)
        assertEquals("А", RuleSort.apply(listOf(a, b), RuleSort.BY_NAME)[0].name)
        assertEquals("А", RuleSort.apply(listOf(a, b), RuleSort.BY_TIME)[0].name)
        assertEquals("А", RuleSort.apply(listOf(a, b), RuleSort.ACTIVE_FIRST)[0].name)
        val rule = GautoRule(name = "домашние дела", blockPackages = listOf("com.google.android.youtube"))
        assertTrue(RuleLogic.matches("ютуб", rule) || RuleLogic.matches("youtube", rule))
        assertTrue(RuleLogic.matches("дела", rule))
        assertFalse(RuleLogic.matches("совсем другое", rule))
    }
}
