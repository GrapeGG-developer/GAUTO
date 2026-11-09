package com.gauto.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GautoContractTest {

    private val main = File("src/main/java/com/gauto/app")
    private fun read(path: String) = File(main, path).readText()

    @Test
    fun bottomNavigationLayoutMatchesTheRequest() {
        val root = read("ui/GautoRoot.kt")
        assertTrue(root.contains("height(80.dp)"))
        assertTrue(root.contains("NavigationBar("))
        assertTrue(root.contains("NavigationBarItem("))
        assertTrue(root.contains("CenterAlignedTopAppBar("))
        assertTrue(root.contains("Icons.Filled.Search"))
        assertTrue(root.contains("Icons.Filled.Notifications"))
        assertTrue(root.contains("Icons.Filled.Person"))
        assertTrue(root.contains("alwaysShowLabel = true"))
        assertTrue(root.contains("GautoTab.entries"))
        assertTrue(root.contains("5 вкладок") || root.contains("GautoTab.HOME"))
        val tabs = read("ui/GautoRoot.kt").substringAfter("enum class GautoTab").substringBefore("\n}")
        listOf("HOME", "RULES", "HABITS", "FOCUS", "SETTINGS").forEach {
            assertTrue("вкладка $it потерялась", tabs.contains(it))
        }
    }

    @Test
    fun gautoIsFullyIndependentFromGsynk() {
        val sources = main.walkTopDown().filter { it.isFile && it.extension == "kt" }.map { it.readText() }.toList()
        assertTrue(sources.isNotEmpty())
        sources.forEach { source ->
            assertFalse("осталась зависимость от GSYNK", source.contains("com.gsynk"))
            assertFalse("вшитый токен", source.contains("ghp_") || source.contains("github_pat_") || source.contains("GHSAT0"))
        }
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains(".core.GAUTOApp"))
        assertTrue(manifest.contains(".core.GautoAccessibilityService"))
        assertTrue(manifest.contains("BIND_ACCESSIBILITY_SERVICE"))
        assertTrue(manifest.contains("SYSTEM_ALERT_WINDOW"))
        assertTrue(manifest.contains("RECEIVE_BOOT_COMPLETED"))
        assertTrue(manifest.contains("SCHEDULE_EXACT_ALARM"))
        assertTrue(manifest.contains("REQUEST_INSTALL_PACKAGES"))
        assertTrue(manifest.contains("screenOrientation=\"portrait\""))
    }

    @Test
    fun blockingAndGrayscaleWorkWithoutAdbOrRoot() {
        val runtime = read("core/GautoRuntime.kt")
        assertTrue(runtime.contains("GLOBAL_ACTION_HOME"))
        assertTrue(runtime.contains("TYPE_WINDOW_STATE_CHANGED"))
        assertTrue(runtime.contains("isPackageBlocked"))
        val overlay = read("core/GrayscaleOverlay.kt")
        assertTrue(overlay.contains("setSaturation(0f)"))
        assertTrue(overlay.contains("TYPE_APPLICATION_OVERLAY"))
        assertTrue(overlay.contains("FLAG_NOT_TOUCHABLE"))
        assertTrue(overlay.contains("FLAG_NOT_FOCUSABLE"))
        assertFalse("root/adb не нужны", overlay.contains("Runtime.getRuntime().exec") || overlay.contains("su -c"))
        val permissions = read("core/GautoPermissions.kt")
        assertTrue(permissions.contains("allRequiredGranted"))
        assertTrue(permissions.contains("ACTION_ACCESSIBILITY_SETTINGS"))
        assertTrue(permissions.contains("ACTION_MANAGE_OVERLAY_PERMISSION"))
    }

    @Test
    fun internalUpdaterHasWatchdogEtagAndDigest() {
        val runtime = read("core/GautoRuntime.kt")
        assertTrue(runtime.contains("delay(200L)"))
        assertTrue(runtime.contains("If-None-Match"))
        assertTrue(runtime.contains("api.github.com/repos"))
        assertTrue(runtime.contains("MessageDigest.getInstance(\"SHA-256\")"))
        assertTrue(runtime.contains("FileProvider.getUriForFile"))
        assertTrue(runtime.contains("cleanupDownloadedUpdates"))
        val models = read("core/GautoModels.kt")
        assertTrue(models.contains("GrapeGG-developer/GAUTO"))
        assertFalse(models.contains("GSYNKAPP"))
        val build = File("build.gradle.kts").readText()
        assertTrue(build.contains("versionCode = 1"))
        assertTrue(build.contains("versionName = \"1.0.0\""))
    }

    @Test
    fun scheduleLogicMovedIntoGauto() {
        val models = read("core/GautoModels.kt")
        listOf("GautoRule", "GautoSettings", "Habit", "FocusSession", "LifePacks", "HabitsLogic", "FocusLogic").forEach {
            assertTrue("модуль $it потерялся", models.contains(it))
        }
        val store = read("core/GautoStore.kt")
        assertTrue(store.contains("fun activeRule("))
        assertTrue(store.contains("fun isPackageBlocked("))
        assertTrue(store.contains("fun installPack("))
        val screens = read("ui/GautoScreens.kt")
        assertTrue(screens.contains("RuleEditorDialog"))
        assertTrue(screens.contains("яркость"))
        assertTrue(screens.contains("громкость"))
    }
}
