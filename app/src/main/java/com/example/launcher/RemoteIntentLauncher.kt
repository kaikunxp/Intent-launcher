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
    val hasLauncherEntry: Boolean = true,
)

data class PackageActivityInfo(
    val className: String,
    val shortName: String,
    val label: String,
    val exported: Boolean,
)

data class AppIntentEntry(
    val title: String,
    val subtitle: String,
    val badge: String,
    val kind: TargetKind,
    val packageName: String,
    val componentClass: String,
    val action: String = "",
    val dataUri: String = "",
    val category: String = "",
    val mimeType: String = "",
    val exported: Boolean = true,
)

data class AppInspectionResult(
    val appLabel: String,
    val packageName: String,
    val intents: List<AppIntentEntry>,
    val activities: List<PackageActivityInfo>,
)

data class QsTileInfo(
    val label: String,
    val packageName: String,
    val serviceClass: String,
    val icon: ImageBitmap?,
    val appLabel: String,
)

object RemoteIntentLauncher {
    const val GOOGLE_TV_PACKAGE = TargetIntentConfig.GOOGLE_TV_PACKAGE
    const val ACTION_CONFIGURE_INTENT = "com.example.action.CONFIGURE_INTENT"
    private const val ACTION_QS_TILE = "android.service.quicksettings.action.QS_TILE"
    private const val ACTION_APPWIDGET_CONFIGURE = "android.appwidget.action.APPWIDGET_CONFIGURE"

    sealed interface LaunchOutcome {
        data object Idle : LaunchOutcome
        data object Launching : LaunchOutcome
        data object Launched : LaunchOutcome
        data class AppNotInstalled(val packageName: String) : LaunchOutcome
        data class ActivityNotFound(val packageName: String, val activityClass: String) : LaunchOutcome
        data class PermissionDenied(val packageName: String, val activityClass: String) : LaunchOutcome
        data class ShizukuInstallRequired(
            val packageName: String,
            val activityClass: String,
            val isQsTile: Boolean,
            val hasShellExtras: Boolean,
        ) : LaunchOutcome
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
        val category = config.category.trim()
        val mimeType = config.mimeType.trim()

        if (pkg.isBlank() && action.isBlank() && dataUri.isBlank()) {
            return null
        }

        if (pkg.isNotBlank() &&
            activity.isBlank() &&
            action.isBlank() &&
            dataUri.isBlank() &&
            category.isBlank() &&
            mimeType.isBlank()
        ) {
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
            val parsedData = if (dataUri.isNotBlank()) Uri.parse(dataUri) else null
            if (parsedData != null && mimeType.isNotBlank()) {
                setDataAndType(parsedData, mimeType)
            } else if (parsedData != null) {
                this.data = parsedData
            } else if (mimeType.isNotBlank()) {
                this.type = mimeType
            }
            if (category.isNotBlank()) {
                addCategory(category)
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

    fun isActivityDeclared(context: Context, packageName: String, activityClass: String): Boolean {
        val pkg = packageName.trim()
        val rawCls = activityClass.trim()
        if (pkg.isBlank() || rawCls.isBlank()) return false
        val resolvedCls = if (rawCls.startsWith(".")) "$pkg$rawCls" else rawCls
        return queryPackageActivities(context, pkg).any { it.className == resolvedCls }
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
        val selfPackage = context.packageName
        val appsByPackage = linkedMapOf<String, InstalledAppInfo>()

        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        queryActivitiesCompat(pm, launcherIntent).forEach { info ->
            val pkg = info.activityInfo?.packageName ?: return@forEach
            if (pkg == selfPackage || appsByPackage.containsKey(pkg)) return@forEach
            val label = info.loadLabel(pm)?.toString().orEmpty().ifBlank { pkg }
            val iconBitmap = runCatching { info.loadIcon(pm)?.toScaledImageBitmap(96) }.getOrNull()
            appsByPackage[pkg] = InstalledAppInfo(
                label = label,
                packageName = pkg,
                launcherActivity = info.activityInfo.name,
                icon = iconBitmap,
                hasLauncherEntry = true,
            )
        }

        queryActivitiesCompat(pm, Intent(Intent.ACTION_MAIN)).forEach { info ->
            val pkg = info.activityInfo?.packageName ?: return@forEach
            if (pkg == selfPackage || appsByPackage.containsKey(pkg)) return@forEach
            val label = info.loadLabel(pm)?.toString().orEmpty().ifBlank { pkg }
            val iconBitmap = runCatching { info.loadIcon(pm)?.toScaledImageBitmap(96) }.getOrNull()
            appsByPackage[pkg] = InstalledAppInfo(
                label = label,
                packageName = pkg,
                launcherActivity = info.activityInfo.name,
                icon = iconBitmap,
                hasLauncherEntry = false,
            )
        }

        queryServicesCompat(pm, Intent(ACTION_QS_TILE)).forEach { info ->
            val pkg = info.serviceInfo?.packageName ?: return@forEach
            if (pkg == selfPackage || appsByPackage.containsKey(pkg)) return@forEach
            val appInfo = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(pkg, 0)
                }
            }.getOrNull()
            val label = appInfo?.let { pm.getApplicationLabel(it).toString() }
                ?.takeIf { it.isNotBlank() }
                ?: pkg
            val iconBitmap = appInfo?.let {
                runCatching { pm.getApplicationIcon(it).toScaledImageBitmap(96) }.getOrNull()
            }
            appsByPackage[pkg] = InstalledAppInfo(
                label = label,
                packageName = pkg,
                launcherActivity = "",
                icon = iconBitmap,
                hasLauncherEntry = false,
            )
        }

        return appsByPackage.values.sortedBy { it.label.lowercase() }
    }

    fun queryQuickSettingsTiles(context: Context): List<QsTileInfo> {
        val pm = context.packageManager
        val tileIntent = Intent(ACTION_QS_TILE)
        return queryServicesCompat(pm, tileIntent)
            .asSequence()
            .filter { it.serviceInfo != null }
            .distinctBy { "${it.serviceInfo.packageName}/${it.serviceInfo.name}" }
            .map { info ->
                val svc = info.serviceInfo
                val rawLabel = info.loadLabel(pm)?.toString().orEmpty()
                    .ifBlank { svc.name.substringAfterLast('.') }
                val appLabel = runCatching {
                    svc.applicationInfo?.let { pm.getApplicationLabel(it).toString() }
                }.getOrNull().orEmpty().ifBlank { svc.packageName }
                val iconBitmap = runCatching {
                    info.loadIcon(pm)?.toScaledImageBitmap(96)
                }.getOrNull()
                QsTileInfo(
                    label = rawLabel,
                    packageName = svc.packageName,
                    serviceClass = svc.name,
                    icon = iconBitmap,
                    appLabel = appLabel,
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun inspectAppPackage(
        context: Context,
        appLabel: String,
        packageName: String,
    ): AppInspectionResult {
        val pm = context.packageManager
        val pkg = packageName.trim()
        val intents = mutableListOf<AppIntentEntry>()
        val seenKeys = mutableSetOf<String>()

        fun addEntry(entry: AppIntentEntry) {
            val key = "${entry.kind}|${entry.packageName}|${entry.componentClass}|${entry.action}|${entry.dataUri}|${entry.category}"
            if (seenKeys.add(key)) {
                intents.add(entry)
            }
        }

        // 1. Main launcher entry
        pm.getLaunchIntentForPackage(pkg)?.component?.let { comp ->
            addEntry(
                AppIntentEntry(
                    title = "Main Launcher",
                    subtitle = comp.className,
                    badge = "LAUNCHER",
                    kind = TargetKind.ACTIVITY,
                    packageName = pkg,
                    componentClass = comp.className,
                    action = Intent.ACTION_MAIN,
                    category = Intent.CATEGORY_LAUNCHER,
                    exported = true,
                ),
            )
        }

        // 2. Quick Settings Tiles belonging to this package
        val pkgTileIntent = Intent(ACTION_QS_TILE).setPackage(pkg)
        queryServicesCompat(pm, pkgTileIntent).forEach { info ->
            val svc = info.serviceInfo ?: return@forEach
            val tileLabel = info.loadLabel(pm)?.toString().orEmpty()
                .ifBlank { svc.name.substringAfterLast('.') }
            addEntry(
                AppIntentEntry(
                    title = tileLabel,
                    subtitle = svc.name,
                    badge = "QS TILE",
                    kind = TargetKind.QS_TILE,
                    packageName = svc.packageName,
                    componentClass = svc.name,
                    exported = true,
                ),
            )
        }

        // 3. Intent-filter discoveries scoped to this package
        val probeIntents = listOf(
            "MAIN" to Intent(Intent.ACTION_MAIN).setPackage(pkg),
            "VIEW URL" to Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                setPackage(pkg)
            },
            "SHARE" to Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage(pkg)
            },
            "WIDGET CONFIG" to Intent(ACTION_APPWIDGET_CONFIGURE).setPackage(pkg),
        )
        for ((badge, probe) in probeIntents) {
            queryActivitiesCompat(pm, probe).forEach { info ->
                val act = info.activityInfo ?: return@forEach
                val actLabel = info.loadLabel(pm)?.toString().orEmpty()
                    .ifBlank { act.name.substringAfterLast('.') }
                val action = probe.action.orEmpty()
                val dataUri = probe.dataString.orEmpty()
                val category = probe.categories?.firstOrNull().orEmpty()
                val mime = probe.type.orEmpty()
                addEntry(
                    AppIntentEntry(
                        title = actLabel,
                        subtitle = act.name,
                        badge = badge,
                        kind = TargetKind.ACTIVITY,
                        packageName = act.packageName,
                        componentClass = act.name,
                        action = action,
                        dataUri = dataUri,
                        category = category,
                        mimeType = mime,
                        exported = act.exported,
                    ),
                )
            }
        }

        // 4. All declared activities (exported & internal)
        val activities = queryPackageActivities(context, pkg)

        return AppInspectionResult(
            appLabel = appLabel,
            packageName = pkg,
            intents = intents,
            activities = activities,
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
            .distinctBy { it.name }
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

    private fun queryActivitiesCompat(
        pm: PackageManager,
        intent: Intent,
    ) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
        )
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
    }

    private fun queryServicesCompat(
        pm: PackageManager,
        intent: Intent,
    ) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentServices(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong()),
        )
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentServices(intent, PackageManager.MATCH_DISABLED_COMPONENTS)
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
