package com.gauto.app

import com.gauto.app.core.GautoRule
import com.gauto.app.core.LifePacks
import com.gauto.app.core.RuleLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LifePacksTest {

    @Test
    fun sixteenPacksGiveSixtyFourDeterministicRules() {
        assertEquals(16, LifePacks.ALL.size)
        assertEquals(64, LifePacks.ALL.sumOf { it.specs.size })
        assertEquals(LifePacks.ALL.size, LifePacks.ALL.map { it.id }.distinct().size)
        LifePacks.ALL.forEach { pack ->
            val first = LifePacks.rules(pack)
            val second = LifePacks.rules(pack)
            assertEquals(first, second)
            assertEquals(4, first.size)
            first.forEach { rule ->
                assertTrue(rule.id.startsWith("life-" + pack.id + "-"))
                assertTrue(rule.brightness in 0..100)
                assertTrue(rule.volume in 0..100)
                assertTrue(rule.days.all { it in 1..7 })
                assertTrue(rule.silentMode in -1..1)
                assertTrue(rule.enabled)
            }
        }
    }

    @Test
    fun choresSleepAndCodePacksReallyBlockAndDim() {
        val chores = LifePacks.byId("chores")!!
        assertTrue(LifePacks.rules(chores).any { it.blockPackages.contains("com.google.android.youtube") })
        val sleep = LifePacks.byId("sleep")!!
        assertTrue(LifePacks.rules(sleep).any { it.grayscale })
        val code = LifePacks.byId("deep-code")!!
        assertTrue(LifePacks.rules(code).any { it.habitId == "code" })
        assertFalse(LifePacks.isInstalled(emptyList(), chores))
        val installed = LifePacks.install(listOf(GautoRule(id = "manual", name = "своё")), chores)
        assertTrue(LifePacks.isInstalled(installed, chores))
        assertTrue(installed.any { it.id == "manual" })
        assertEquals(5, installed.size)
    }

    @Test
    fun defaultHabitsCoverTheWholeLoop() {
        val habits = LifePacks.defaultHabits()
        assertEquals(7, habits.size)
        assertEquals(habits.size, habits.map { it.id }.distinct().size)
        listOf("chores", "sleep", "code", "read", "sport", "plan", "detox").forEach { id ->
            assertNotNull("нет привычки $id", habits.firstOrNull { it.id == id })
        }
        assertTrue(RuleLogic.DAY_FULL.size == 7)
    }
}
