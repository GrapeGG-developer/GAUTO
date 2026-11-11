package com.gauto.app

import com.gauto.app.core.GautoRepo
import com.gauto.app.core.UpdateLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateLogicTest {

    private val latestJson = """
        {
          "tag_name": "v1.0.1",
          "name": "GAUTO 1.0.1",
          "draft": false,
          "prerelease": false,
          "assets": [
            {
              "name": "GAUTO-v1.0.1-debug.apk",
              "size": 123456,
              "browser_download_url": "https://github.com/GrapeGG-developer/GAUTO/releases/download/v1.0.1/GAUTO-v1.0.1-debug.apk",
              "digest": "sha256:abcdef0123456789"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun publicApiIsParsedWithoutAnyToken() {
        val update = UpdateLogic.parseLatest(latestJson)
        assertNotNull(update)
        assertEquals("v1.0.1", update!!.tag)
        assertEquals(123456L, update.size)
        assertEquals("abcdef0123456789", update.sha256)
        assertTrue(update.apkUrl.contains("GAUTO/releases/download/v1.0.1"))
    }

    @Test
    fun draftsPrereleasesAndGarbageAreIgnored() {
        assertNull(UpdateLogic.parseLatest(latestJson.replace("\"draft\": false", "\"draft\": true")))
        assertNull(UpdateLogic.parseLatest(latestJson.replace("\"prerelease\": false", "\"prerelease\": true")))
        assertNull(UpdateLogic.parseLatest("просто текст"))
        assertNull(UpdateLogic.parseLatest("{}"))
    }

    @Test
    fun versionComparisonUsesSemverNotBuildCode() {
        assertEquals(10000, UpdateLogic.versionCodeFromTag("v1.0.0"))
        assertEquals(10203, UpdateLogic.versionCodeFromTag("v1.2.3"))
        assertEquals(0, UpdateLogic.versionCodeFromTag("мусор"))
        assertFalse(UpdateLogic.isNewer("v1.0.0"))
        assertFalse(UpdateLogic.isNewer("v2.0.0"))
        assertFalse(UpdateLogic.isNewer("v3.0.0"))
        assertTrue(UpdateLogic.isNewer("v3.0.1"))
        assertTrue(UpdateLogic.isNewer("v3.1.0"))
        assertFalse(UpdateLogic.isNewer("v0.9.9"))
    }

    @Test
    fun urlsAndDigestsStayInsideGautoRepo() {
        assertEquals("sha256:abc", "sha256:abc")
        assertEquals("abc", UpdateLogic.shaFromDigest("sha256:abc"))
        assertEquals("", UpdateLogic.shaFromDigest(null))
        assertTrue(UpdateLogic.apkUrl(GautoRepo.REPO, "v1.0.1").contains("GrapeGG-developer/GAUTO"))
        assertEquals("GAUTO-v1.0.1-debug.apk", UpdateLogic.safeApkFileName("v1.0.1"))
        assertEquals("GrapeGG-developer/GAUTO", GautoRepo.REPO)
        assertEquals("3.0.0", GautoRepo.VERSION)
    }
}
