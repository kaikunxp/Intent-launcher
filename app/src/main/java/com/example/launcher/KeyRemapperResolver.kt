package com.example.launcher

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Immutable

@Immutable
data class RemapperTarget(
    val title: String,
    val subtitle: String,
    val label: String,
    val config: TargetIntentConfig,
    val preflight: PreflightResult,
)

@Immutable
data class ResolvedKeyRemapper(
    val primary: RemapperTarget,
    val fallback: RemapperTarget? = null,
)

internal data class RemapperProbeSnapshot(
    val manufacturer: String,
    val brand: String,
    val motoKeyPlus: RemapperTarget? = null,
    val motorolaStock: RemapperTarget? = null,
    val samsungSideKey: RemapperTarget? = null,
    val samsungRegistar: RemapperTarget? = null,
    val pixelGesture: RemapperTarget? = null,
    val thirdPartyRemapper: RemapperTarget? = null,
)

object KeyRemapperResolver {
    const val MOTO_KEY_PLUS_PACKAGE = "app.xandar.motokeyplus"
    const val SAMSUNG_SETTINGS_PACKAGE = "com.android.settings"
    const val SAMSUNG_SIDEKEY_PACKAGE = "com.samsung.android.sidekey"
    const val SAMSUNG_REGISTAR_PACKAGE = "com.samsung.android.app.registar"
    const val GOOGLE_SETTINGS_PACKAGE = "com.google.android.settings"
    const val KEY_MAPPER_PACKAGE = "io.github.sds100.keymapper"
    const val BUTTON_MAPPER_PACKAGE = "flar2.homebutton"

    private val samsungExactActivities = listOf(
        "com.android.settings.Settings\$FunctionKeySettingsActivity",
        "com.android.settings.Settings\$SideKeySettingsActivity",
        "com.android.settings.Settings\$ActiveKeySettingsActivity",
        "com.android.settings.Settings\$XCoverKeySettingsActivity",
        "com.android.settings.Settings\$TopKeySettingsActivity",
        "com.android.settings.Settings\$BixbyKeySettingsActivity",
    )

    private val pixelExactActivities = listOf(
        "com.android.settings.Settings\$ColumbusSettingsActivity",
        "com.android.settings.gestures.ColumbusSettingsActivity",
        "com.android.settings.Settings\$DoubleTapPowerSettingsActivity",
        "com.android.settings.gestures.DoubleTapPowerSettingsActivity",
    )

    fun resolve(context: Context): ResolvedKeyRemapper? {
        val snapshot = RemapperProbeSnapshot(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            brand = Build.BRAND.orEmpty(),
            motoKeyPlus = findMotoKeyPlusTarget(context),
            motorolaStock = findMotorolaStockTarget(context),
            samsungSideKey = findSamsungNativeTarget(context),
            samsungRegistar = findLaunchableAppTarget(
                context = context,
                packageName = SAMSUNG_REGISTAR_PACKAGE,
                displayLabel = "Good Lock RegiStar",
                subtitle = "Open Good Lock RegiStar",
            ),
            pixelGesture = findPixelGestureTarget(context),
            thirdPartyRemapper = findThirdPartyRemapperTarget(context),
        )
        return resolveFromSnapshot(snapshot)
    }

    internal fun resolveFromSnapshot(snapshot: RemapperProbeSnapshot): ResolvedKeyRemapper? {
        val mfg = snapshot.manufacturer.lowercase()
        val brand = snapshot.brand.lowercase()
        val isMotorola = mfg.contains("motorola") || brand.contains("motorola") ||
            snapshot.motorolaStock != null || snapshot.motoKeyPlus != null
        val isSamsung = mfg.contains("samsung") || brand.contains("samsung")
        val isGoogle = mfg.contains("google") || brand.contains("google")

        if (isMotorola) {
            val motoPrimary = snapshot.motoKeyPlus ?: snapshot.motorolaStock
            if (motoPrimary != null) {
                val motoFallback = if (snapshot.motoKeyPlus != null) {
                    snapshot.motorolaStock ?: snapshot.thirdPartyRemapper
                } else {
                    snapshot.thirdPartyRemapper
                }
                return ResolvedKeyRemapper(primary = motoPrimary, fallback = motoFallback)
            }
        }

        if (isSamsung) {
            val samsungPrimary = snapshot.samsungSideKey ?: snapshot.samsungRegistar
            if (samsungPrimary != null) {
                val samsungFallback = if (snapshot.samsungSideKey != null) {
                    snapshot.samsungRegistar ?: snapshot.thirdPartyRemapper
                } else {
                    snapshot.thirdPartyRemapper
                }
                return ResolvedKeyRemapper(primary = samsungPrimary, fallback = samsungFallback)
            }
        }

        if (isGoogle) {
            val pixelPrimary = snapshot.pixelGesture ?: snapshot.thirdPartyRemapper
            if (pixelPrimary != null) {
                val pixelFallback = if (snapshot.pixelGesture != null) {
                    snapshot.thirdPartyRemapper
                } else {
                    null
                }
                return ResolvedKeyRemapper(primary = pixelPrimary, fallback = pixelFallback)
            }
        }

        val fallbackOnly = snapshot.motoKeyPlus
            ?: snapshot.motorolaStock
            ?: snapshot.samsungSideKey
            ?: snapshot.samsungRegistar
            ?: snapshot.pixelGesture
            ?: snapshot.thirdPartyRemapper
            ?: return null

        return ResolvedKeyRemapper(primary = fallbackOnly, fallback = null)
    }

    private fun findMotoKeyPlusTarget(context: Context): RemapperTarget? {
        findLaunchableAppTarget(
            context = context,
            packageName = MOTO_KEY_PLUS_PACKAGE,
            displayLabel = "MotoKey+",
            subtitle = "Open MotoKey+ remapper",
        )?.let { return it }

        val apps = RemoteIntentLauncher.queryLaunchableApps(context)
        val match = apps.firstOrNull { app ->
            app.packageName != TargetIntentConfig.MOTOROLA_MYKEY_PACKAGE &&
                (
                    app.packageName == MOTO_KEY_PLUS_PACKAGE ||
                        app.label.contains("motokey+", ignoreCase = true) ||
                        app.label.contains("motokey plus", ignoreCase = true)
                    )
        } ?: apps.firstOrNull { app ->
            app.packageName != TargetIntentConfig.MOTOROLA_MYKEY_PACKAGE &&
                (
                    app.label.equals("MotoKey", ignoreCase = true) ||
                        app.packageName.contains("motokey", ignoreCase = true)
                    )
        } ?: return null

        val activityClass = match.launcherActivity.ifBlank {
            val declared = RemoteIntentLauncher.queryPackageActivities(context, match.packageName)
            declared.firstOrNull { it.exported }?.className
                ?: declared.firstOrNull()?.className.orEmpty()
        }

        val config = TargetIntentConfig(
            label = match.label,
            packageName = match.packageName,
            activityClass = activityClass,
            autoLaunchEnabled = false,
            preferPrivilegedLaunch = true,
            kind = TargetKind.ACTIVITY,
            isConfigured = false,
        )
        return RemapperTarget(
            title = "Remap Hardware Key",
            subtitle = "Open ${match.label} remapper",
            label = match.label,
            config = config,
            preflight = RemoteIntentLauncher.checkNativeCompatibility(context, config),
        )
    }

    private fun findMotorolaStockTarget(context: Context): RemapperTarget? {
        if (!RemoteIntentLauncher.isPackageInstalled(context, TargetIntentConfig.MOTOROLA_MYKEY_PACKAGE)) {
            return null
        }
        val declared = RemoteIntentLauncher.queryPackageActivities(
            context,
            TargetIntentConfig.MOTOROLA_MYKEY_PACKAGE,
        )
        val activityClass = declared.firstOrNull {
            it.className == TargetIntentConfig.MOTOROLA_RED_KEY_ACTIVITY
        }?.className
            ?: declared.firstOrNull {
                it.shortName.contains("RedKey", ignoreCase = true) ||
                    it.shortName.contains("MyKey", ignoreCase = true) ||
                    it.shortName.contains("ActionKey", ignoreCase = true)
            }?.className
            ?: TargetIntentConfig.MOTOROLA_RED_KEY_ACTIVITY

        val config = TargetIntentConfig.MotorolaRedKeyRemapper.copy(activityClass = activityClass)
        return RemapperTarget(
            title = "Remap Hardware Key",
            subtitle = "Open Motorola Key settings (${activityClass.substringAfterLast('.')})",
            label = config.label,
            config = config,
            preflight = RemoteIntentLauncher.checkNativeCompatibility(context, config),
        )
    }

    private fun findSamsungNativeTarget(context: Context): RemapperTarget? {
        val mfg = Build.MANUFACTURER.orEmpty().lowercase()
        val brand = Build.BRAND.orEmpty().lowercase()
        if (!mfg.contains("samsung") && !brand.contains("samsung")) {
            return null
        }

        findLaunchableAppTarget(
            context = context,
            packageName = SAMSUNG_SIDEKEY_PACKAGE,
            displayLabel = "Samsung Side Button",
            subtitle = "Open Samsung Side button settings",
        )?.let { return it }

        val settingsActivities = RemoteIntentLauncher.queryPackageActivities(
            context,
            SAMSUNG_SETTINGS_PACKAGE,
        )
        if (settingsActivities.isEmpty()) return null

        val exactMatch = samsungExactActivities.firstNotNullOfOrNull { targetClass ->
            settingsActivities.firstOrNull { it.className == targetClass }
        } ?: settingsActivities.firstOrNull { info ->
            val short = info.shortName
            short.contains("FunctionKey", ignoreCase = true) ||
                short.contains("SideKey", ignoreCase = true) ||
                short.contains("SideButton", ignoreCase = true) ||
                short.contains("ActiveKey", ignoreCase = true) ||
                short.contains("XCoverKey", ignoreCase = true) ||
                short.contains("TopKey", ignoreCase = true)
        }

        if (exactMatch != null) {
            val isRuggedKey = exactMatch.shortName.contains("ActiveKey", ignoreCase = true) ||
                exactMatch.shortName.contains("XCoverKey", ignoreCase = true) ||
                exactMatch.shortName.contains("TopKey", ignoreCase = true)
            val label = if (isRuggedKey) "Samsung Active / XCover Key" else "Samsung Side Button"
            val subtitle = if (isRuggedKey) {
                "Open Samsung XCover / Active key settings"
            } else {
                "Open Samsung Side button settings"
            }
            val config = TargetIntentConfig(
                label = label,
                packageName = SAMSUNG_SETTINGS_PACKAGE,
                activityClass = exactMatch.className,
                autoLaunchEnabled = false,
                preferPrivilegedLaunch = true,
                kind = TargetKind.ACTIVITY,
                isConfigured = false,
            )
            return RemapperTarget(
                title = "Remap Hardware Key",
                subtitle = subtitle,
                label = label,
                config = config,
                preflight = RemoteIntentLauncher.checkNativeCompatibility(context, config),
            )
        }

        // One UI 5/6/7+ frequently routes Side button via SubSettings fragment arguments
        val subSettings = settingsActivities.firstOrNull {
            it.className == "com.android.settings.SubSettings" ||
                it.shortName.equals("SubSettings", ignoreCase = true)
        } ?: return null

        val config = TargetIntentConfig(
            label = "Samsung Side Button",
            packageName = SAMSUNG_SETTINGS_PACKAGE,
            activityClass = subSettings.className,
            extraArgs = "--es :android:show_fragment com.samsung.android.settings.usefulfeature.FunctionKeySettings",
            autoLaunchEnabled = false,
            preferPrivilegedLaunch = true,
            kind = TargetKind.ACTIVITY,
            isConfigured = false,
        )
        return RemapperTarget(
            title = "Remap Hardware Key",
            subtitle = "Open Samsung Side button settings",
            label = "Samsung Side Button",
            config = config,
            preflight = RemoteIntentLauncher.checkNativeCompatibility(context, config),
        )
    }

    private fun findPixelGestureTarget(context: Context): RemapperTarget? {
        val mfg = Build.MANUFACTURER.orEmpty().lowercase()
        val brand = Build.BRAND.orEmpty().lowercase()
        if (!mfg.contains("google") && !brand.contains("google")) {
            return null
        }

        val candidatePackages = listOf(SAMSUNG_SETTINGS_PACKAGE, GOOGLE_SETTINGS_PACKAGE)
            .filter { RemoteIntentLauncher.isPackageInstalled(context, it) }

        for (pkg in candidatePackages) {
            val activities = RemoteIntentLauncher.queryPackageActivities(context, pkg)
            val match = pixelExactActivities.firstNotNullOfOrNull { exact ->
                activities.firstOrNull { it.className == exact }
            } ?: activities.firstOrNull {
                it.shortName.contains("ColumbusSettings", ignoreCase = true) ||
                    it.shortName.contains("DoubleTapPowerSettings", ignoreCase = true)
            }
            if (match != null) {
                val isQuickTap = match.shortName.contains("Columbus", ignoreCase = true)
                val label = if (isQuickTap) "Pixel Quick Tap" else "Power Button Shortcut"
                val subtitle = if (isQuickTap) {
                    "Open Pixel Quick Tap gesture settings"
                } else {
                    "Open Press & hold power button settings"
                }
                val config = TargetIntentConfig(
                    label = label,
                    packageName = pkg,
                    activityClass = match.className,
                    autoLaunchEnabled = false,
                    preferPrivilegedLaunch = true,
                    kind = TargetKind.ACTIVITY,
                    isConfigured = false,
                )
                return RemapperTarget(
                    title = "Remap Hardware Key",
                    subtitle = subtitle,
                    label = label,
                    config = config,
                    preflight = RemoteIntentLauncher.checkNativeCompatibility(context, config),
                )
            }
        }
        return null
    }

    private fun findThirdPartyRemapperTarget(context: Context): RemapperTarget? {
        findMotoKeyPlusTarget(context)?.let { return it }
        findLaunchableAppTarget(
            context = context,
            packageName = KEY_MAPPER_PACKAGE,
            displayLabel = "Key Mapper",
            subtitle = "Open Key Mapper app",
        )?.let { return it }
        findLaunchableAppTarget(
            context = context,
            packageName = BUTTON_MAPPER_PACKAGE,
            displayLabel = "Button Mapper",
            subtitle = "Open Button Mapper app",
        )?.let { return it }
        return null
    }

    private fun findLaunchableAppTarget(
        context: Context,
        packageName: String,
        displayLabel: String,
        subtitle: String,
    ): RemapperTarget? {
        if (!RemoteIntentLauncher.isPackageInstalled(context, packageName)) {
            return null
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        var activityClass = launchIntent?.component?.className.orEmpty()
        if (activityClass.isBlank()) {
            val declared = RemoteIntentLauncher.queryPackageActivities(context, packageName)
            activityClass = declared.firstOrNull {
                it.exported && (
                    it.shortName.contains("Main", ignoreCase = true) ||
                        it.shortName.contains("Home", ignoreCase = true) ||
                        it.shortName.contains("Splash", ignoreCase = true) ||
                        it.shortName.contains("Setting", ignoreCase = true) ||
                        it.shortName.contains("Config", ignoreCase = true) ||
                        it.shortName.contains("Remap", ignoreCase = true) ||
                        it.shortName.contains("Setup", ignoreCase = true)
                    )
            }?.className
                ?: declared.firstOrNull { it.exported }?.className
                ?: declared.firstOrNull()?.className
                ?: return null
        }
        val config = TargetIntentConfig(
            label = displayLabel,
            packageName = packageName,
            activityClass = activityClass,
            autoLaunchEnabled = false,
            preferPrivilegedLaunch = true,
            kind = TargetKind.ACTIVITY,
            isConfigured = false,
        )
        return RemapperTarget(
            title = "Remap Hardware Key",
            subtitle = subtitle,
            label = displayLabel,
            config = config,
            preflight = RemoteIntentLauncher.checkNativeCompatibility(context, config),
        )
    }
}
