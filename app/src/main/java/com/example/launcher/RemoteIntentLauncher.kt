package com.example.launcher

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap

data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val launcherActivity: String,
    val icon: ImageBitmap?,
)

data class PackageActivityInfo(
    val className: String,
    val shortName: String,
    val label: String,
    val exported: Boolean,
)

data class QsTileInfo(
    val label: String,
    val packageName: String,
    val serviceClass: String,
    val icon: ImageBitmap?,
    val isRemoteCandidate: Boolean,
)

data class RemoteActivityCandidate(
    val appLabel: String,
    val packageName: String,
    val className: String,
    val shortName: String,
    val exported: Boolean,
)

object RemoteIntentLauncher {
    const val GOOGLE_TV_PACKAGE = TargetIntentConfig.DEFAULT_PACKAGE
    const val GOOGLE_HOME_PACKAGE = "com.google.android.apps.chromecast.app"
    const val PLAY_SERVICES_PACKAGE = "com.google.android.gms"
    const val ACTION_CONFIGURE_INTENT = "com.example.action.CONFIGURE_INTENT"
    private const val ACTION_QS_TILE = "android.service.quicksettings.action.QS_TILE"

    sealed interface LaunchOutcome {
        data object Idle : LaunchOutcome
        data object Launching : LaunchOutcome
        data object Launched : LaunchOutcome
        data class AppNotInstalled(val packageName: String) : LaunchOutcome
        data class ActivityNotFound(val packageName: String, val activityClass: String) : LaunchOutcome
        data class PermissionDenied(val packageName: String, val activityClass: String) : LaunchOutcome
        data object SheveryStubMissing : LaunchOutcome
        data object SheveryServiceNotRunning : LaunchOutcome
        data object SheveryPermissionRequired : LaunchOutcome
        data object InvalidTarget : LaunchOutcome
        data class Failed(val detail: String?) : LaunchOutcome
    }

    fun buildIntent(context: Context, config: TargetIntentConfig): Intent? {
        if (config.kind == TargetKind.QS_TILE) {
            return null
        }

        val pkg = config.packageName.trim()
        val activity = config.activityClass.trim()
        val action = config.action.trim()
        val dataUri = config.dataUri.trim()

        if (pkg.isBlank() && action.isBlank() && dataUri.isBlank()) {
            return null
        }

        if (pkg.isNotBlank() && activity.isBlank() && action.isBlank() && dataUri.isBlank()) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return launchIntent
            }
            return Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        return Intent().apply {
            if (action.isNotBlank()) {
                this.action = action
            }
            if (dataUri.isNotBlank()) {
                this.data = Uri.parse(dataUri)
            }
            if (pkg.isNotBlank() && activity.isNotBlank()) {
                val resolvedClass = if (activity.startsWith(".")) "$pkg$activity" else activity
                component = ComponentName(pkg, resolvedClass)
            } else if (pkg.isNotBlank()) {
                setPackage(pkg)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        val trimmed = packageName.trim()
        if (trimmed.isBlank()) return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    trimmed,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(trimmed, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun launchTarget(context: Context, config: TargetIntentConfig): LaunchOutcome {
        if (config.kind == TargetKind.QS_TILE) {
            return if (config.isValid) {
                LaunchOutcome.PermissionDenied(config.packageName, config.activityClass)
            } else {
                LaunchOutcome.InvalidTarget
            }
        }

        val intent = buildIntent(context, config) ?: return LaunchOutcome.InvalidTarget
        val pkg = config.packageName.trim()
        val activity = config.activityClass.trim()

        return try {
            context.startActivity(intent)
            LaunchOutcome.Launched
        } catch (_: ActivityNotFoundException) {
            if (pkg.isNotBlank() && !isPackageInstalled(context, pkg)) {
                LaunchOutcome.AppNotInstalled(pkg)
            } else {
                LaunchOutcome.ActivityNotFound(pkg, activity)
            }
        } catch (_: SecurityException) {
            LaunchOutcome.PermissionDenied(pkg, activity)
        } catch (e: Exception) {
            LaunchOutcome.Failed(e.localizedMessage)
        }
    }

    fun launchGoogleTvApp(context: Context): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(GOOGLE_TV_PACKAGE)
            ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launchIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openPackageInPlayStore(context: Context, packageName: String): Boolean {
        val pkg = packageName.trim().ifBlank { GOOGLE_TV_PACKAGE }
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$pkg"),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(marketIntent)
            true
        } catch (_: ActivityNotFoundException) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$pkg"),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun queryLaunchableApps(context: Context): List<InstalledAppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                mainIntent,
                PackageManager.ResolveInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        val selfPackage = context.packageName
        return resolveInfos
            .asSequence()
            .filter { it.activityInfo?.packageName != null && it.activityInfo.packageName != selfPackage }
            .distinctBy { "${it.activityInfo.packageName}/${it.activityInfo.name}" }
            .map { info ->
                val label = info.loadLabel(pm)?.toString().orEmpty()
                    .ifBlank { info.activityInfo.packageName }
                val iconBitmap = runCatching {
                    info.loadIcon(pm)?.toScaledImageBitmap(sizePx = 96)
                }.getOrNull()
                InstalledAppInfo(
                    label = label,
                    packageName = info.activityInfo.packageName,
                    launcherActivity = info.activityInfo.name,
                    icon = iconBitmap,
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun queryQuickSettingsTiles(context: Context): List<QsTileInfo> {
        val pm = context.packageManager
        val tileIntent = Intent(ACTION_QS_TILE)
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentServices(
                tileIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentServices(tileIntent, PackageManager.MATCH_DISABLED_COMPONENTS)
        }

        return resolveInfos
            .asSequence()
            .filter { it.serviceInfo != null }
            .distinctBy { "${it.serviceInfo.packageName}/${it.serviceInfo.name}" }
            .map { info ->
                val svc = info.serviceInfo
                val rawLabel = info.loadLabel(pm)?.toString().orEmpty()
                    .ifBlank { svc.name.substringAfterLast('.') }
                val haystack = "${rawLabel.lowercase()} ${svc.packageName.lowercase()} ${svc.name.lowercase()}"
                val isRemote = haystack.contains("remote") ||
                    svc.packageName == GOOGLE_TV_PACKAGE ||
                    (haystack.contains("tv") && (svc.packageName.startsWith("com.google.") || haystack.contains("control")))
                val iconBitmap = runCatching {
                    info.loadIcon(pm)?.toScaledImageBitmap(sizePx = 96)
                }.getOrNull()
                QsTileInfo(
                    label = rawLabel,
                    packageName = svc.packageName,
                    serviceClass = svc.name,
                    icon = iconBitmap,
                    isRemoteCandidate = isRemote,
                )
            }
            .sortedWith(
                compareByDescending<QsTileInfo> { it.isRemoteCandidate }
                    .thenBy { it.label.lowercase() },
            )
            .toList()
    }

    fun findRemoteActivitiesOnDevice(context: Context): List<RemoteActivityCandidate> {
        val packagesToScan = listOf(
            GOOGLE_TV_PACKAGE to "Google TV",
            GOOGLE_HOME_PACKAGE to "Google Home",
            PLAY_SERVICES_PACKAGE to "Google Play Services",
        )
        val results = mutableListOf<RemoteActivityCandidate>()
        for ((pkg, appLabel) in packagesToScan) {
            if (!isPackageInstalled(context, pkg)) continue
            val activities = queryPackageActivities(context, pkg)
            activities
                .filter { act ->
                    val lower = act.className.lowercase()
                    lower.contains("remote") && !lower.contains("test")
                }
                .forEach { act ->
                    results.add(
                        RemoteActivityCandidate(
                            appLabel = appLabel,
                            packageName = pkg,
                            className = act.className,
                            shortName = act.shortName,
                            exported = act.exported,
                        ),
                    )
                }
        }
        return results.sortedWith(
            compareByDescending<RemoteActivityCandidate> { it.packageName == GOOGLE_TV_PACKAGE }
                .thenByDescending { it.shortName.contains("Device", ignoreCase = true) || it.shortName.contains("Remote", ignoreCase = true) }
                .thenByDescending { it.exported }
                .thenBy { it.shortName },
        )
    }

    fun queryPackageActivities(context: Context, packageName: String): List<PackageActivityInfo> {
        val pkg = packageName.trim()
        if (pkg.isBlank()) return emptyList()
        val pm = context.packageManager
        val packageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    pkg,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
            }
        } catch (_: Exception) {
            return emptyList()
        }

        val activities = packageInfo.activities ?: return emptyList()
        return activities
            .map { activityInfo ->
                val className = activityInfo.name
                val shortName = className.substringAfterLast('.')
                val label = runCatching { activityInfo.loadLabel(pm)?.toString() }.getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: shortName
                PackageActivityInfo(
                    className = className,
                    shortName = shortName,
                    label = label,
                    exported = activityInfo.exported,
                )
            }
            .sortedWith(compareByDescending<PackageActivityInfo> { it.exported }.thenBy { it.shortName })
    }

    private fun Drawable.toScaledImageBitmap(sizePx: Int): ImageBitmap {
        if (this is BitmapDrawable && bitmap != null) {
            val scaled = Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
            return scaled.asImageBitmap()
        }
        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap.asImageBitmap()
    }
}
