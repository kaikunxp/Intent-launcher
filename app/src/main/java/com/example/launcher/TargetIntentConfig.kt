package com.example.launcher

data class TargetIntentConfig(
    val label: String = "",
    val packageName: String = "",
    val activityClass: String = "",
    val action: String = "",
    val dataUri: String = "",
    val category: String = "",
    val mimeType: String = "",
    val extraArgs: String = "",
    val autoLaunchEnabled: Boolean = true,
    val preferPrivilegedLaunch: Boolean = true,
    val retryTileIfNoScreen: Boolean = true,
    val kind: TargetKind = TargetKind.ACTIVITY,
    val isConfigured: Boolean = false,
) {
    val displayTarget: String
        get() = when {
            kind == TargetKind.QS_TILE && packageName.isNotBlank() && activityClass.isNotBlank() ->
                "cmd statusbar click-tile $packageName/$activityClass"
            packageName.isNotBlank() && activityClass.isNotBlank() ->
                buildList {
                    add("$packageName/$activityClass")
                    if (action.isNotBlank()) add("($action)")
                    if (dataUri.isNotBlank()) add(dataUri)
                    if (extraArgs.isNotBlank()) add(extraArgs)
                }.joinToString(" ")
            packageName.isNotBlank() && action.isNotBlank() ->
                listOfNotNull(
                    "$packageName ($action)",
                    dataUri.takeIf { it.isNotBlank() },
                    extraArgs.takeIf { it.isNotBlank() },
                ).joinToString(" ")
            packageName.isNotBlank() -> packageName
            action.isNotBlank() && dataUri.isNotBlank() -> "$action • $dataUri"
            action.isNotBlank() -> action
            dataUri.isNotBlank() -> dataUri
            else -> "No intent selected yet"
        }

    val isValid: Boolean
        get() = when (kind) {
            TargetKind.QS_TILE -> packageName.isNotBlank() && activityClass.isNotBlank()
            TargetKind.ACTIVITY ->
                packageName.isNotBlank() || action.isNotBlank() || dataUri.isNotBlank()
        }

    companion object {
        const val GOOGLE_TV_PACKAGE = "com.google.android.videos"
        const val GOOGLE_TV_REMOTE_LEGACY_ACTIVITY =
            "com.google.android.apps.play.movies.common.remote.RemoteDevicesListActivity"
        const val DEFAULT_PACKAGE = GOOGLE_TV_PACKAGE
        const val MOTOROLA_MYKEY_PACKAGE = "com.motorola.mykey"
        const val MOTOROLA_RED_KEY_ACTIVITY = "com.motorola.mykey.activity.RedKeyActivity"

        val Default = TargetIntentConfig()

        val MotorolaRedKeyRemapper = TargetIntentConfig(
            label = "Motorola Key Remapper",
            packageName = MOTOROLA_MYKEY_PACKAGE,
            activityClass = MOTOROLA_RED_KEY_ACTIVITY,
            autoLaunchEnabled = false,
            preferPrivilegedLaunch = true,
            kind = TargetKind.ACTIVITY,
            isConfigured = false,
        )

        val Presets: List<IntentPreset> = listOf(
            IntentPreset(
                id = "google_tv_app",
                title = "Google TV",
                subtitle = "Opens Google TV (built-in TV remote icon inside)",
                config = TargetIntentConfig(
                    label = "Google TV",
                    packageName = GOOGLE_TV_PACKAGE,
                    autoLaunchEnabled = true,
                    isConfigured = true,
                ),
            ),
            IntentPreset(
                id = "google_home_app",
                title = "Google Home",
                subtitle = "Smart home & Google TV device remote controls",
                config = TargetIntentConfig(
                    label = "Google Home",
                    packageName = "com.google.android.apps.chromecast.app",
                    autoLaunchEnabled = true,
                    isConfigured = true,
                ),
            ),
            IntentPreset(
                id = "developer_options",
                title = "Developer Options",
                subtitle = "Jump directly to Android Developer Options",
                config = TargetIntentConfig(
                    label = "Developer Options",
                    action = "android.settings.APPLICATION_DEVELOPMENT_SETTINGS",
                    autoLaunchEnabled = true,
                    isConfigured = true,
                ),
            ),
            IntentPreset(
                id = "wireless_debugging",
                title = "Wireless Debugging",
                subtitle = "System Wireless Debugging settings (Shevery / Shizuku)",
                config = TargetIntentConfig(
                    label = "Wireless Debugging",
                    packageName = "com.android.settings",
                    activityClass = "com.android.settings.Settings\$WirelessDebuggingActivity",
                    autoLaunchEnabled = true,
                    preferPrivilegedLaunch = true,
                    isConfigured = true,
                ),
            ),
            IntentPreset(
                id = "connected_devices",
                title = "Connected Devices",
                subtitle = "Cast, Bluetooth & paired devices",
                config = TargetIntentConfig(
                    label = "Connected Devices",
                    packageName = "com.android.settings",
                    activityClass = "com.android.settings.Settings\$ConnectedDeviceDashboardActivity",
                    autoLaunchEnabled = true,
                    isConfigured = true,
                ),
            ),
            IntentPreset(
                id = "notification_history",
                title = "Notification History",
                subtitle = "Android Notification History log",
                config = TargetIntentConfig(
                    label = "Notification History",
                    action = "android.settings.NOTIFICATION_HISTORY",
                    autoLaunchEnabled = true,
                    isConfigured = true,
                ),
            ),
            IntentPreset(
                id = "google_tv_remote_legacy",
                title = "Google TV Remote (Legacy Activity)",
                subtitle = "Legacy RemoteDevicesListActivity (older Google TV builds)",
                config = TargetIntentConfig(
                    label = "Google TV Remote",
                    packageName = GOOGLE_TV_PACKAGE,
                    activityClass = GOOGLE_TV_REMOTE_LEGACY_ACTIVITY,
                    autoLaunchEnabled = true,
                    preferPrivilegedLaunch = true,
                    isConfigured = true,
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
                    isConfigured = true,
                ),
            ),
        )
    }
}

enum class TargetKind {
    ACTIVITY,
    QS_TILE,
}

data class IntentPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val config: TargetIntentConfig,
)
