package com.gauto.app

import com.gauto.app.core.AlarmLogic
import com.gauto.app.core.AlarmRules
import com.gauto.app.core.BlockMode
import com.gauto.app.core.GautoAlarm
import com.gauto.app.core.GautoReminder
import com.gauto.app.core.GautoTimer
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmLogicTest {

    private val zone = ZoneId.of("Europe/Moscow")
    private fun at(day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, zone)

    @Test
    fun noSnoozeIsAProductRule() {
        assertTrue(AlarmRules.NO_SNOOZE)
        assertTrue(AlarmLogic.validateAlarm(GautoAlarm(strict = false)).strict)
    }

    @Test
    fun nextTriggerRespectsDaysAndSkipsPastTime() {
        val weekdays = GautoAlarm(hour = 7, minute = 0, days = listOf(1, 2, 3, 4, 5))
        val sunday = at(8, 6, 30) // воскресенье
        val next = AlarmLogic.nextTrigger(weekdays, sunday)
        assertTrue(next != null && next.isAfter(sunday))
        assertTrue(next!!.dayOfWeek.value in 1..5)
        assertEquals(7, next.hour)
        assertNull(AlarmLogic.nextTrigger(weekdays.copy(enabled = false), sunday))
        assertNull(AlarmLogic.nextTrigger(weekdays.copy(days = emptyList()), sunday))
    }

    @Test
    fun sunriseStartsBeforeTheAlarm() {
        val alarm = GautoAlarm(hour = 7, minute = 0, sunriseMinutes = 10)
        val now = at(9, 6, 0)
        val trigger = AlarmLogic.nextTrigger(alarm, now)!!
        val sunrise = AlarmLogic.sunriseAt(alarm, now)!!
        assertEquals(10L, java.time.Duration.between(sunrise, trigger).toMinutes())
        assertTrue(sunrise.isAfter(now))
    }

    @Test
    fun volumeRampsUpAndAutoStopIsNotSnooze() {
        assertEquals(AlarmLogic.escalationVolume(0, 30, 80) < 80, true)
        assertEquals(80, AlarmLogic.escalationVolume(30, 30, 80))
        assertEquals(80, AlarmLogic.escalationVolume(999, 30, 80))
        assertTrue(AlarmLogic.escalationVolume(15, 30, 80) > AlarmLogic.escalationVolume(0, 30, 80))
        assertFalse(AlarmLogic.autoStopReached(59, 1))
        assertTrue(AlarmLogic.autoStopReached(60, 1))
        assertTrue(AlarmLogic.autoStopReached(300, 5))
    }

    @Test
    fun timerFormatAndValidation() {
        assertEquals("00:05", AlarmLogic.formatTimer(5))
        assertEquals("25:00", AlarmLogic.formatTimer(1500))
        assertEquals("1:01:01", AlarmLogic.formatTimer(3661))
        assertEquals(5, AlarmLogic.validateTimer(GautoTimer(durationSec = 1)).durationSec)
        assertEquals(24 * 3600, AlarmLogic.validateTimer(GautoTimer(durationSec = 999999)).durationSec)
        assertEquals(100, AlarmLogic.validateTimer(GautoTimer(volume = 999)).volume)
    }

    @Test
    fun fourthAxisIsTimeOfDay() {
        assertEquals(0f, AlarmLogic.phaseOfDay(LocalTime.of(0, 0)), 0.001f)
        assertEquals(0.5f, AlarmLogic.phaseOfDay(LocalTime.of(12, 0)), 0.001f)
        assertEquals("рассвет", AlarmLogic.sceneTitles(0.27f))
        assertEquals("день", AlarmLogic.sceneTitles(0.6f))
    }

    @Test
    fun remindersAndValidation() {
        val reminder = GautoReminder(hour = 12, minute = 0)
        val next = AlarmLogic.nextReminder(reminder, at(10, 11, 0))
        assertEquals(12, next?.hour)
        assertEquals(23, AlarmLogic.validateReminder(GautoReminder(hour = 99)).hour)
        assertEquals(listOf(1, 7), AlarmLogic.validateReminder(GautoReminder(days = listOf(9, 7, 7, 1))).days)
        assertEquals(BlockMode.ALARM.title, "до подъёма")
    }

    @Test
    fun validateAlarmClampsEverything() {
        val clean = AlarmLogic.validateAlarm(
            GautoAlarm(hour = 99, minute = -3, volume = 500, rampSec = 0, sunriseMinutes = 99,
                autoStopMinutes = 0, wakeBrightness = 0, sound = "мусор", strict = false)
        )
        assertEquals(23, clean.hour)
        assertEquals(0, clean.minute)
        assertEquals(100, clean.volume)
        assertEquals(5, clean.rampSec)
        assertEquals(30, clean.sunriseMinutes)
        assertEquals(1, clean.autoStopMinutes)
        assertEquals(1, clean.wakeBrightness)
        assertEquals(AlarmRules.SOUND_SUNRISE, clean.sound)
        assertTrue(clean.strict)
    }
}
