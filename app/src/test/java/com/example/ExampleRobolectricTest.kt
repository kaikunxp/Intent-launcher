package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.launcher.RemoteIntentLauncher
import com.example.launcher.SheveryBridge
import com.example.launcher.TargetIntentConfig
import com.example.launcher.TargetKind
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Button Intent Launcher`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Button Intent Launcher", appName)
    }

    @Test
    fun `default target intent is empty until configured`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = RemoteIntentLauncher.buildIntent(context, TargetIntentConfig.Default)
        assertNull(intent)
    }

    @Test
    fun `legacy google tv remote preset targets RemoteDevicesListActivity`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val legacyPreset = TargetIntentConfig.Presets.first { it.id == "google_tv_remote_legacy" }
        val intent = RemoteIntentLauncher.buildIntent(context, legacyPreset.config)
        assertNotNull(intent)
        assertEquals("com.google.android.videos", intent?.component?.packageName)
        assertEquals(
            "com.google.android.apps.play.movies.common.remote.RemoteDevicesListActivity",
            intent?.component?.className,
        )
        assertTrue(((intent?.flags ?: 0) and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
    }

    @Test
    fun `qs tile privileged args build cmd statusbar click-tile`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val args = SheveryBridge.buildPrivilegedArgs(
            context,
            TargetIntentConfig(
                label = "TV Remote",
                packageName = "com.google.android.videos",
                activityClass = "com.google.android.apps.play.movies.common.remote.RemoteTileService",
                kind = TargetKind.QS_TILE,
            ),
        )
        assertArrayEquals(
            arrayOf(
                "cmd",
                "statusbar",
                "click-tile",
                "com.google.android.videos/com.google.android.apps.play.movies.common.remote.RemoteTileService",
            ),
            args,
        )
    }

    @Test
    fun `empty target returns null intent`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = RemoteIntentLauncher.buildIntent(
            context,
            TargetIntentConfig(
                label = "",
                packageName = "",
                activityClass = "",
                action = "",
                dataUri = "",
            ),
        )
        assertNull(intent)
    }
}
