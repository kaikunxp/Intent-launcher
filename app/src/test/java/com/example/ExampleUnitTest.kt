package com.example

import com.example.launcher.AdbCommandParser
import com.example.launcher.TargetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
}
