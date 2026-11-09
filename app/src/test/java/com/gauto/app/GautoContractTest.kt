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
        assertTrue(root.contains("ic_gauto_logo"))
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
        assertTrue(models.contains("Gauto Island UI 2.0"))
        assertTrue(models.contains("const val VERSION = \"2.0.0\""))
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
    fun versionAndUpdaterAreConsistent() {
        val build = File("build.gradle.kts").readText()
        assertTrue(build.contains("versionCode = 2"))
        assertTrue(build.contains("versionName = \"2.0.0\""))
        val runtime = read("core/GautoRuntime.kt")
        assertTrue(runtime.contains("delay(200L)"))
        assertTrue(runtime.contains("If-None-Match"))
        assertTrue(runtime.contains("MessageDigest.getInstance(\"SHA-256\")"))
    }
}
