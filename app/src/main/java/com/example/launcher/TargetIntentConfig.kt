package com.example.launcher

enum class TargetKind {
    ACTIVITY,
    QS_TILE,
}

data class TargetIntentConfig(
    val label: String = DEFAULT_LABEL,
    val packageName: String = DEFAULT_PACKAGE,
    val activityClass: String = DEFAULT_ACTIVITY,
    val action: String = "",
    val dataUri: String = "",
    val autoLaunchEnabled: Boolean = true,
    val preferPrivilegedLaunch: Boolean = true,
    val kind: TargetKind = TargetKind.ACTIVITY,
) {
    val displayTarget: String
        get() = when {
            kind == TargetKind.QS_TILE && packageName.isNotBlank() && activityClass.isNotBlank() ->
                "QS Tile • $packageName/$activityClass"
            packageName.isNotBlank() && activityClass.isNotBlank() -> "$packageName/$activityClass"
            packageName.isNotBlank() && action.isNotBlank() -> "$packageName ($action)"
            packageName.isNotBlank() -> packageName
            action.isNotBlank() && dataUri.isNotBlank() -> "$action • $dataUri"
            action.isNotBlank() -> action
            dataUri.isNotBlank() -> dataUri
            else -> "Not configured"
        }

    val isValid: Boolean
        get() = when (kind) {
            TargetKind.QS_TILE -> packageName.isNotBlank() && activityClass.isNotBlank()
            TargetKind.ACTIVITY ->
                packageName.isNotBlank() || action.isNotBlank() || dataUri.isNotBlank()
        }

    companion object {
        const val DEFAULT_LABEL = "Google TV Remote"
        const val DEFAULT_PACKAGE = "com.google.android.videos"
        const val DEFAULT_ACTIVITY =
            "com.google.android.apps.play.movies.common.remote.RemoteDevicesListActivity"

        val Default = TargetIntentConfig()

        val Presets: List<IntentPreset> = listOf(
            IntentPreset(
                id = "google_tv_remote",
                title = "Google TV Remote (Legacy Activity)",
                subtitle = "Direct jump to RemoteDevicesListActivity",
                config = TargetIntentConfig(
                    label = "Google TV Remote",
                    packageName = DEFAULT_PACKAGE,
                    activityClass = DEFAULT_ACTIVITY,
                    autoLaunchEnabled = true,
                ),
            ),
            IntentPreset(
                id = "google_tv_app",
                title = "Google TV App",
                subtitle = "Opens Google TV (tap the Remote button inside)",
                config = TargetIntentConfig(
                    label = "Google TV",
                    packageName = DEFAULT_PACKAGE,
                    activityClass = "",
                    action = "",
                    autoLaunchEnabled = true,
                ),
            ),
            IntentPreset(
                id = "google_home_app",
                title = "Google Home",
                subtitle = "Smart home & built-in TV remote controls",
                config = TargetIntentConfig(
                    label = "Google Home",
                    packageName = "com.google.android.apps.chromecast.app",
                    activityClass = "",
                    action = "",
                    autoLaunchEnabled = true,
                ),
            ),
            IntentPreset(
                id = "connected_devices",
                title = "Connected Devices",
                subtitle = "Android Cast, Bluetooth & paired devices",
                config = TargetIntentConfig(
                    label = "Connected Devices",
                    packageName = "com.android.settings",
                    activityClass = "com.android.settings.Settings\$ConnectedDeviceDashboardActivity",
                    autoLaunchEnabled = true,
                ),
            ),
            IntentPreset(
                id = "android_settings",
                title = "Android Settings",
                subtitle = "System Settings main screen",
                config = TargetIntentConfig(
                    label = "Settings",
                    packageName = "com.android.settings",
                    activityClass = "com.android.settings.Settings",
                    autoLaunchEnabled = true,
                ),
            ),
        )
    }
}

data class IntentPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val config: TargetIntentConfig,
)
