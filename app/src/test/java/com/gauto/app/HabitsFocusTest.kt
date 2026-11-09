package com.gauto.app

import com.gauto.app.core.FocusLogic
import com.gauto.app.core.FocusSession
import com.gauto.app.core.Habit
import com.gauto.app.core.HabitsLogic
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitsFocusTest {

    @Test
    fun streakCountsConsecutiveDaysBackwards() {
        val today = LocalDate.of(2026, 10, 12)
        val dates = listOf("2026-10-12", "2026-10-11", "2026-10-10", "2026-10-08")
        assertEquals(3, HabitsLogic.streak(dates, today))
        assertEquals(0, HabitsLogic.streak(emptyList<String>(), today))
        assertEquals(1, HabitsLogic.streak(listOf("2026-10-12"), today))
    }

    @Test
    fun weekProgressAndLevelAreBounded() {
        val habit = Habit(targetPerWeek = 5, doneDates = listOf("2026-10-12", "2026-10-11", "2026-10-01"))
        assertEquals(2, HabitsLogic.weekProgress(habit, LocalDate.of(2026, 10, 12)))
        assertEquals(1, HabitsLogic.level(0))
        assertTrue(HabitsLogic.level(200) > HabitsLogic.level(0))
        assertTrue(HabitsLogic.progress(0) in 0f..1f)
        assertTrue(HabitsLogic.progress(999999) in 0f..1f)
        assertTrue(HabitsLogic.title(0).isNotBlank())
        assertTrue(HabitsLogic.xpFor(habit, 3) > habit.xp)
    }

    @Test
    fun focusSessionHasInertiaBoundsAndReward() {
        val now = 1_000_000L
        val active = FocusSession(startedAt = now - 60_000L, minutes = 25)
        assertTrue(FocusLogic.isActive(active, now))
        assertEquals(24 * 60L, FocusLogic.remainingSeconds(active, now))
        val expired = FocusSession(startedAt = now - 60 * 60_000L, minutes = 25)
        assertFalse(FocusLogic.isActive(expired, now))
        assertEquals(0L, FocusLogic.remainingSeconds(expired, now))
        val done = active.copy(completed = true)
        assertFalse(FocusLogic.isActive(done, now))
        assertEquals("01:30", FocusLogic.label(90L))
        assertEquals(50, FocusLogic.xp(25))
    }
}
