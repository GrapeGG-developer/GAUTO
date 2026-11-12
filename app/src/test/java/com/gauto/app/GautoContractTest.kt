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
        listOf("HOME", "ALARM", "RULES", "MORE").forEach {
            assertTrue("вкладка $it потерялась", tabs.contains(it))
        }
        assertTrue(root.contains("Icons.Filled.Search"))
        assertTrue(root.contains("Icons.Filled.Notifications"))
        assertTrue(root.contains("GautoAnimatedLogo("))
        assertFalse(root.contains("GautoTab.HABITS"))
        assertFalse(root.contains("GautoTab.FOCUS"))
        assertFalse(root.contains("GautoTab.TIMERS"))
        assertFalse(root.contains("GautoTab.REMINDERS"))
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains(".core.GautoWidget"))
        assertTrue(manifest.contains("@xml/gauto_widget_info"))
        assertTrue(File("src/main/res/layout/gauto_widget.xml").exists())
        assertTrue(read("ui/GautoRoot.kt").contains("GautoOnboarding {"))
    }

    @Test
    fun islandIsReallyAnimated() {
        val island = read("ui/GautoIsland.kt")
        listOf("Animatable(0f)", "spring(", "tabs.forEachIndexed", "GautoAnimatedLogo(", ".clickable(enabled = p > 0.5f)").forEach {
            assertTrue("остров потерял $it", island.contains(it))
        }
        assertFalse("частицы/волны снова загромождают остров", island.contains("waveTransition") || island.contains("drawLine("))
        val root = read("ui/GautoRoot.kt")
        assertTrue(root.contains("AnimatedContent("))
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
        assertTrue(models.contains("Gauto Clear UI 5.0"))
        assertTrue(models.contains("const val VERSION = \"5.0.0\""))
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
    fun materialYouAndSimpleIslandArePresent() {
        val theme = read("ui/GautoTheme.kt")
        listOf("dynamicDarkColorScheme", "LocalGautoAccents", "LocalGautoMotion", "Build.VERSION_CODES.S").forEach {
            assertTrue("Material You потерял $it", theme.contains(it))
        }
        val logo = read("ui/GautoAnimatedLogo.kt")
        listOf("rememberInfiniteTransition", "drawRoundRect", "drawCircle", "LocalGautoAccents").forEach { assertTrue(logo.contains(it)) }
        val island = read("ui/GautoIsland.kt")
        listOf("Animatable(0f)", "spring(", "tabs.forEachIndexed", "GautoAnimatedLogo(").forEach { assertTrue(island.contains(it)) }
        assertFalse(island.contains("waveTransition") || island.contains("drawLine("))
        val widget = read("core/GautoWidget.kt")
        assertTrue(widget.contains("RemoteViews"))
        assertTrue(widget.contains("ACTION_FOCUS"))
        assertTrue(File("src/main/res/layout/gauto_widget.xml").exists())
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
        assertTrue("авто-стоп должен отличаться от переноса", ringer.contains("autoStopReached"))
    }

    @Test
    fun fourSectionsKeepAllSpecializedFunctionsUnderMore() {
        val root = read("ui/GautoRoot.kt")
        listOf("GautoTab.HOME", "GautoTab.ALARM", "GautoTab.RULES", "GautoTab.MORE").forEach { assertTrue(root.contains(it)) }
        val more = read("ui/MoreScreen.kt")
        listOf("MorePage.TIMERS", "MorePage.REMINDERS", "MorePage.HABITS", "MorePage.FOCUS", "MorePage.SETTINGS", "BackupScreen(").forEach { assertTrue(more.contains(it)) }
        val fourD = read("ui/Gauto4D.kt")
        listOf("cameraDistance", "rotationX", "rotationY", "phaseOfDay").forEach { assertTrue(fourD.contains(it)) }
        val protocol = read("core/SyncProtocol.kt")
        assertTrue(protocol.contains("const val SCHEMA = 2"))
        listOf("alarms", "timers", "reminders").forEach { assertTrue(protocol.contains(it)) }
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains(".ui.AlarmOverlayActivity"))
        assertTrue(manifest.contains(".core.AlarmReceiver"))
        assertTrue(manifest.contains(".core.GautoWidget"))
        assertTrue(manifest.contains("USE_FULL_SCREEN_INTENT"))
    }

    @Test
    fun onboardingAndWidgetMakeFirstUseClear() {
        val onboarding = read("ui/GautoOnboarding.kt")
        listOf("Начни с главного", "Поставь подъём", "Убери отвлечения", "Синхронизация с ПК", "GautoOnboarding").forEach {
            assertTrue("онбординг не объясняет $it", onboarding.contains(it))
        }
        val widget = read("core/GautoWidget.kt")
        listOf("GautoWidget", "ACTION_OPEN", "ACTION_FOCUS", "ACTION_SLEEP", "ACTION_QUIET", "RemoteViews").forEach {
            assertTrue("виджет потерял $it", widget.contains(it))
        }
        val widgetXml = File("src/main/res/layout/gauto_widget.xml").readText()
        listOf("widget_alarm", "widget_focus", "widget_sleep", "widget_quiet").forEach {
            assertTrue("виджет потерял $it", widgetXml.contains(it))
        }
    }

    @Test
    fun todayIsShortAndMoreContainsSecondarySections() {
        val today = read("ui/TodayScreen.kt")
        listOf("Сегодня", "Ближайший подъём", "Быстрые действия", "Фокус 25 мин", "Тишина 30 мин", "Сон").forEach {
            assertTrue("Сегодня потерял $it", today.contains(it))
        }
        val more = read("ui/MoreScreen.kt")
        listOf("Таймеры", "Напоминания", "Привычки", "Фокус-сессии", "Настройки", "Резервная копия").forEach {
            assertTrue("раздел «Ещё» потерял $it", more.contains(it))
        }
        val rules = read("ui/GautoScreens.kt")
        assertTrue(rules.contains("Готовые планы ·"))
        assertTrue(rules.contains("showPacks"))
    }

    @Test
    fun noSnoozeAndWidgetHasOnlyThreeQuickActions() {
        val overlay = read("ui/AlarmOverlayActivity.kt")
        listOf("Отложить", "отложить", "snooze", "10 минут").forEach { assertFalse(overlay.contains(it)) }
        val widget = read("core/GautoWidget.kt")
        listOf("ACTION_FOCUS", "ACTION_SLEEP", "ACTION_QUIET", "widget_alarm").forEach { assertTrue(widget.contains(it)) }
        val xml = File("src/main/res/layout/gauto_widget.xml").readText()
        listOf("widget_focus", "widget_sleep", "widget_quiet").forEach { assertTrue(xml.contains(it)) }
    }

    @Test
    fun versionAndUpdaterAreConsistent() {
        val build = File("build.gradle.kts").readText()
        assertTrue(build.contains("versionCode = 5"))
        assertTrue(build.contains("versionName = \"5.0.0\""))
        val runtime = read("core/GautoRuntime.kt")
        assertTrue(runtime.contains("delay(200L)"))
        assertTrue(runtime.contains("If-None-Match"))
        assertTrue(runtime.contains("MessageDigest.getInstance(\"SHA-256\")"))
    }
}
