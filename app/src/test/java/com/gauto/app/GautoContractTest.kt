package com.gauto.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GautoContractTest {

    private val main = File("src/main/java/com/gauto/app")
    private fun read(path: String) = File(main, path).readText()

    @Test
    fun islandReplacesTheBoringBottomBar() {
        val root = read("ui/GautoRoot.kt")
        listOf("GautoIsland(", "GautoIslandBackground(", "expanded", "onToggle", "GautoTopBar(").forEach {
            assertTrue("остров потерял $it", root.contains(it))
        }
        listOf("NavigationBar(", "NavigationBarItem(", "height(80.dp)", "alwaysShowLabel").forEach {
            assertFalse("вернулся скучный бар: $it", root.contains(it))
        }
        val tabs = root.substringAfter("enum class GautoTab").substringBefore("\n}")
        listOf("HOME", "RULES", "HABITS", "FOCUS", "SETTINGS").forEach {
            assertTrue("вкладка $it потерялась", tabs.contains(it))
        }
        assertTrue(root.contains("Icons.Filled.Search"))
        assertTrue(root.contains("Icons.Filled.Notifications"))
        assertTrue(root.contains("GautoAnimatedLogo("))
    }

    @Test
    fun islandIsReallyAnimated() {
        val island = read("ui/GautoIsland.kt")
        listOf(
            "Animatable(0f)", "spring(", "infiniteRepeatable(", "AnimatedContent(",
            "graphicsLayer {", "drawGlyph(", "LaunchedEffect(expanded)",
            "Brush.radialGradient(", "translationX =", "rotationZ ="
        ).forEach { marker -> assertTrue("анимация $marker потерялась", island.contains(marker)) }
        val root = read("ui/GautoRoot.kt")
        assertTrue(root.contains("AnimatedContent("))
        assertTrue(root.contains("slideInVertically"))
        assertTrue(root.contains("scaleIn("))
    }

    @Test
    fun nameIsGautoEverywhereVisible() {
        val strings = File("src/main/res/values/strings.xml").readText()
        assertTrue(strings.contains("<string name=\"app_name\">Gauto</string>"))
        assertFalse(strings.contains(">GAUTO<"))
        val screens = read("ui/GautoScreens.kt") + read("ui/GautoComponents.kt") + read("ui/GautoRoot.kt")
        assertFalse("осталось старое написание в UI", screens.contains("\"GAUTO") || screens.contains("GAUTO ·"))
        val models = read("core/GautoModels.kt")
        assertTrue(models.contains("Gauto 4D Living Island 4.0"))
        assertTrue(models.contains("const val VERSION = \"4.0.0\""))
        assertTrue(File("branding/gauto_logo.svg").exists() || File("../branding/gauto_logo.svg").exists())
    }

    @Test
    fun lanSyncWorksWithoutCloud() {
        val protocol = read("core/SyncProtocol.kt")
        listOf("PORT = 8765", "DISCOVERY_PORT = 8766", "GAUTO_DISCOVER_v1", "GAUTO_HERE_v1", "X-Gauto-Pin", "fun merge(", "fun parseHere(").forEach {
            assertTrue("протокол потерял $it", protocol.contains(it))
        }
        val service = read("core/GautoSyncService.kt")
        listOf("ServerSocket", "DatagramSocket", "/api/state", "/api/ping", "startForeground", "localIpAddress()").forEach {
            assertTrue("сервис потерял $it", service.contains(it))
        }
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains(".core.GautoSyncService"))
        assertTrue(manifest.contains("ACCESS_WIFI_STATE"))
        val settings = read("ui/GautoScreens.kt")
        assertTrue(settings.contains("синхронизация с ПК"))
        assertTrue(settings.contains("GautoSyncService.start(context)"))
        val sources = main.walkTopDown().filter { it.isFile && it.extension == "kt" }.map { it.readText() }.toList()
        sources.forEach {
            assertFalse("вшитый токен", it.contains("ghp_") || it.contains("github_pat_") || it.contains("GHSAT0"))
            assertFalse("зависимость от GSYNK", it.contains("com.gsynk"))
        }
    }

    @Test
    fun materialYouAnimatedLogoAndMaximumMotion() {
        val theme = read("ui/GautoTheme.kt")
        listOf("dynamicDarkColorScheme", "LocalGautoAccents", "LocalGautoMotion", "Build.VERSION_CODES.S").forEach {
            assertTrue("Material You потерял $it", theme.contains(it))
        }
        val logo = read("ui/GautoAnimatedLogo.kt")
        listOf("rememberInfiniteTransition", "drawRoundRect", "drawCircle", "LocalGautoAccents", "LocalGautoMotion").forEach {
            assertTrue("анимированный логотип потерял $it", logo.contains(it))
        }
        val island = read("ui/GautoIsland.kt")
        listOf(
            "petalProgress", "delay(", "statusCarousel", "statuses", "waveTransition",
            "GautoAnimatedLogo(", "LocalHapticFeedback", "LocalGautoMotion", "drawLine("
        ).forEach { marker -> assertTrue("максимум движения потерял $marker", island.contains(marker)) }
        val root = read("ui/GautoRoot.kt")
        assertTrue(root.contains("GautoAnimatedLogo("))
        assertTrue(root.contains("GautoTheme(motion = motion)"))
        assertTrue(root.contains("LocalGautoAccents"))
        assertTrue(root.contains("statuses = statuses"))
        val screens = read("ui/GautoScreens.kt")
        assertTrue(screens.contains("живость островка"))
        assertTrue(screens.contains("motionLevel"))
        val models = read("core/GautoModels.kt")
        assertTrue(models.contains("motionLevel"))
    }

    @Test
    fun alarmOverlayHasNoSnoozeAtAll() {
        val overlay = File("src/main/java/com/gauto/app/ui/AlarmOverlayActivity.kt").readText()
        listOf("Я встал", "onLongPress", "BackHandler", "Gauto4DBackdrop", "AlarmRinger.stop", "WakeEffects.confirm").forEach {
            assertTrue("оверлей потерял $it", overlay.contains(it))
        }
        listOf("Отложить", "отложить", "snooze", "Snooze", "SNOOZE", "на 10 минут", "10 минут").forEach {
            assertFalse("в оверлей вернулся snooze: $it", overlay.contains(it))
        }
        val runtime = read("core/AlarmRuntime.kt")
        assertTrue(runtime.contains("setFullScreenIntent"))
        assertTrue(runtime.contains("ACTION_SUNRISE"))
        assertTrue(runtime.contains("ACTION_TIMER_FIRE"))
        assertTrue(runtime.contains("ACTION_REMINDER_FIRE"))
        val ringer = read("core/AlarmRinger.kt")
        assertTrue(ringer.contains("USAGE_ALARM"))
        assertTrue(ringer.contains("escalationVolume"))
        assertTrue(ringer.contains("autoStopReached"))
        assertFalse("звук не должен предлагать отложить", ringer.contains("snooze"))
    }

    @Test
    fun unifiedUiHasEightTabsAndAllNewBlocks() {
        val root = read("ui/GautoRoot.kt")
        listOf("GautoTab.ALARM", "GautoTab.TIMERS", "GautoTab.REMINDERS", "AlarmScreen(", "TimersScreen(", "RemindersScreen(", "Gauto4DBackdrop(", "fourDEnabled").forEach {
            assertTrue("единый UI потерял $it", root.contains(it))
        }
        val fourD = read("ui/Gauto4D.kt")
        listOf("cameraDistance", "rotationX", "rotationY", "phaseOfDay", "timeAxis").forEach {
            assertTrue("4D-слой потерял $it", fourD.contains(it))
        }
        val protocol = read("core/SyncProtocol.kt")
        assertTrue(protocol.contains("const val SCHEMA = 2"))
        listOf("alarms", "timers", "reminders").forEach { assertTrue(protocol.contains(it)) }
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains(".ui.AlarmOverlayActivity"))
        assertTrue(manifest.contains(".core.AlarmReceiver"))
        assertTrue(manifest.contains("USE_FULL_SCREEN_INTENT"))
        val island = read("ui/GautoIsland.kt")
        assertTrue(island.contains("tabs.size - 1"))
    }

    @Test
    fun versionAndUpdaterAreConsistent() {
        val build = File("build.gradle.kts").readText()
        assertTrue(build.contains("versionCode = 4"))
        assertTrue(build.contains("versionName = \"4.0.0\""))
        val runtime = read("core/GautoRuntime.kt")
        assertTrue(runtime.contains("delay(200L)"))
        assertTrue(runtime.contains("If-None-Match"))
        assertTrue(runtime.contains("MessageDigest.getInstance(\"SHA-256\")"))
    }
}
