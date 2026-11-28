package com.example

import com.example.launcher.AdbCommandParser
import com.example.launcher.InstalledAppInfo
import com.example.launcher.KeyRemapperResolver
import com.example.launcher.PreflightResult
import com.example.launcher.QsTileInfo
import com.example.launcher.RemapperProbeSnapshot
import com.example.launcher.RemapperTarget
import com.example.launcher.TargetIntentConfig
import com.example.launcher.TargetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun parseAdbShellAmStartComponent() {
        val result = AdbCommandParser.parse(
            "adb shell am start -n com.google.android.videos/com.google.android.apps.play.movies.common.remote.RemoteDevicesListActivity",
        )
        assertNull(result.error)
        val cfg = result.config
        assertNotNull(cfg)
        assertEquals(TargetKind.ACTIVITY, cfg?.kind)
        assertEquals("com.google.android.videos", cfg?.packageName)
        assertEquals(
            "com.google.android.apps.play.movies.common.remote.RemoteDevicesListActivity",
            cfg?.activityClass,
        )
    }

    @Test
    fun parseAdbShellRelativeActivityAndExtras() {
        val result = AdbCommandParser.parse(
            "adb shell am start --user 0 -a android.intent.action.VIEW -n com.android.settings/.Settings --es extra_key \"hello world\"",
        )
        assertNull(result.error)
        val cfg = result.config
        assertNotNull(cfg)
        assertEquals("android.intent.action.VIEW", cfg?.action)
        assertEquals("com.android.settings", cfg?.packageName)
        assertEquals("com.android.settings.Settings", cfg?.activityClass)
        assertEquals("--es extra_key \"hello world\"", cfg?.extraArgs)
    }

    @Test
    fun parseAdbClickTileCommand() {
        val result = AdbCommandParser.parse(
            "adb shell cmd statusbar click-tile com.google.android.videos/.RemoteTileService",
        )
        assertNull(result.error)
        val cfg = result.config
        assertNotNull(cfg)
        assertEquals(TargetKind.QS_TILE, cfg?.kind)
        assertEquals("com.google.android.videos", cfg?.packageName)
        assertEquals("com.google.android.videos.RemoteTileService", cfg?.activityClass)
    }

    @Test
    fun parseBareComponentAndAction() {
        val comp = AdbCommandParser.parse("com.android.settings/.Settings\$WifiSettingsActivity")
        assertEquals("com.android.settings", comp.config?.packageName)
        assertEquals(
            "com.android.settings.Settings\$WifiSettingsActivity",
            comp.config?.activityClass,
        )

        val act = AdbCommandParser.parse("android.settings.APPLICATION_DEVELOPMENT_SETTINGS")
        assertEquals("android.settings.APPLICATION_DEVELOPMENT_SETTINGS", act.config?.action)
    }

    @Test
    fun motorolaPrefersMotoKeyPlusOverStockRedKey() {
        val motoKeyPlus = sampleTarget("MotoKey+", KeyRemapperResolver.MOTO_KEY_PLUS_PACKAGE)
        assertEquals("app.xandar.motokeyplus", KeyRemapperResolver.MOTO_KEY_PLUS_PACKAGE)
        val motoStock = sampleTarget("Motorola Key Remapper", TargetIntentConfig.MOTOROLA_MYKEY_PACKAGE)

        val resolved = KeyRemapperResolver.resolveFromSnapshot(
            RemapperProbeSnapshot(
                manufacturer = "motorola",
                brand = "motorola",
                motoKeyPlus = motoKeyPlus,
                motorolaStock = motoStock,
            ),
        )
        assertEquals(motoKeyPlus, resolved?.primary)
        assertEquals(motoStock, resolved?.fallback)

        val stockOnly = KeyRemapperResolver.resolveFromSnapshot(
            RemapperProbeSnapshot(
                manufacturer = "motorola",
                brand = "motorola",
                motoKeyPlus = null,
                motorolaStock = motoStock,
            ),
        )
        assertEquals(motoStock, stockOnly?.primary)
        assertNull(stockOnly?.fallback)
    }

    @Test
    fun samsungPrefersSideButtonWithRegistarFallback() {
        val sideButton = sampleTarget("Samsung Side Button", "com.android.settings")
        val registar = sampleTarget("Good Lock RegiStar", KeyRemapperResolver.SAMSUNG_REGISTAR_PACKAGE)

        val both = KeyRemapperResolver.resolveFromSnapshot(
            RemapperProbeSnapshot(
                manufacturer = "samsung",
                brand = "samsung",
                samsungSideKey = sideButton,
                samsungRegistar = registar,
            ),
        )
        assertEquals(sideButton, both?.primary)
        assertEquals(registar, both?.fallback)

        val registarOnly = KeyRemapperResolver.resolveFromSnapshot(
            RemapperProbeSnapshot(
                manufacturer = "samsung",
                brand = "samsung",
                samsungSideKey = null,
                samsungRegistar = registar,
            ),
        )
        assertEquals(registar, registarOnly?.primary)
    }

    @Test
    fun hidesRemapperWhenNoExactTargetExists() {
        val resolved = KeyRemapperResolver.resolveFromSnapshot(
            RemapperProbeSnapshot(
                manufacturer = "Generic",
                brand = "Android",
            ),
        )
        assertNull(resolved)
    }

    @Test
    fun precomputedSearchKeyMatchesAppAndTileFields() {
        val app = InstalledAppInfo(
            label = "MotoKey+",
            packageName = "app.xandar.motokeyplus",
            launcherActivity = "app.xandar.motokeyplus.MainActivity",
            icon = null,
        )
        assertTrue(app.searchKey.contains("motokey+"))
        assertTrue(app.searchKey.contains("app.xandar.motokeyplus"))
        assertFalse(app.searchKey.contains("samsung"))

        val tile = QsTileInfo(
            label = "TV Remote",
            packageName = "com.google.android.videos",
            serviceClass = "com.google.android.apps.play.movies.common.remote.RemoteTileService",
            icon = null,
            appLabel = "Google TV",
        )
        assertTrue(tile.searchKey.contains("tv remote"))
        assertTrue(tile.searchKey.contains("google tv"))
        assertTrue(tile.searchKey.contains("remotetileservice"))
    }

    private fun sampleTarget(label: String, pkg: String): RemapperTarget =
        RemapperTarget(
            title = "Remap Hardware Key",
            subtitle = label,
            label = label,
            config = TargetIntentConfig(label = label, packageName = pkg),
            preflight = PreflightResult(requiresShizuku = false),
        )
}
