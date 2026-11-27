package com.example.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import com.example.BuildConfig
import com.example.IRemoteShellService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

object SheveryBridge {
    const val SHEVERY_PACKAGE = "com.hamondev.shevery"
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val PERMISSION_REQUEST_CODE = 4201

    enum class BridgeStatus {
        READY,
        PERMISSION_REQUIRED,
        STUB_MISSING,
        SERVICE_NOT_RUNNING,
        NOT_INSTALLED,
    }

    fun isSheveryInstalled(context: Context): Boolean =
        RemoteIntentLauncher.isPackageInstalled(context, SHEVERY_PACKAGE)

    fun isShizukuOrStubInstalled(context: Context): Boolean =
        RemoteIntentLauncher.isPackageInstalled(context, SHIZUKU_PACKAGE)

    fun isBinderAlive(): Boolean =
        try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }

    fun hasPermission(): Boolean =
        try {
            isBinderAlive() &&
                !Shizuku.isPreV11() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }

    fun getDiagnosticLine(context: Context): String {
        val shevery = if (isSheveryInstalled(context)) "Installed" else "Missing"
        val stub = if (isShizukuOrStubInstalled(context)) "Installed" else "Missing"
        val binder = if (isBinderAlive()) "Connected" else "Disconnected"
        val auth = if (hasPermission()) "Granted" else "Not granted"
        return "Shevery: $shevery • Stub: $stub • Binder: $binder • Auth: $auth"
    }

    fun getStatus(context: Context): BridgeStatus {
        if (isBinderAlive()) {
            return if (hasPermission()) BridgeStatus.READY else BridgeStatus.PERMISSION_REQUIRED
        }
        val sheveryInstalled = isSheveryInstalled(context)
        val stubInstalled = isShizukuOrStubInstalled(context)
        return when {
            sheveryInstalled && !stubInstalled -> BridgeStatus.STUB_MISSING
            sheveryInstalled || stubInstalled -> BridgeStatus.SERVICE_NOT_RUNNING
            else -> BridgeStatus.NOT_INSTALLED
        }
    }

    fun isConnected(status: BridgeStatus): Boolean =
        status == BridgeStatus.READY || status == BridgeStatus.PERMISSION_REQUIRED

    fun openSheveryOrShizuku(context: Context): Boolean {
        val targetPkg = when {
            isSheveryInstalled(context) -> SHEVERY_PACKAGE
            isShizukuOrStubInstalled(context) -> SHIZUKU_PACKAGE
            else -> return false
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPkg)
            ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launchIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openShizukuInstall(context: Context): Boolean =
        RemoteIntentLauncher.openPackageInPlayStore(context, SHIZUKU_PACKAGE)

    fun openSheveryReleases(context: Context): Boolean {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://github.com/HmnDev-Tech/shevery/releases"),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun awaitBinderAvailable(timeoutMs: Long = 2_000L): Boolean {
        if (isBinderAlive()) return true
        val received = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Boolean> { cont ->
                val listener = object : Shizuku.OnBinderReceivedListener {
                    override fun onBinderReceived() {
                        Shizuku.removeBinderReceivedListener(this)
                        if (cont.isActive) {
                            cont.resume(true)
                        }
                    }
                }
                Shizuku.addBinderReceivedListenerSticky(listener)
                cont.invokeOnCancellation {
                    Shizuku.removeBinderReceivedListener(listener)
                }
            }
        }
        return received == true && isBinderAlive()
    }

    suspend fun requestPermissionAndAwait(timeoutMs: Long = 30_000L): Boolean {
        if (!isBinderAlive() || Shizuku.isPreV11()) return false
        if (hasPermission()) return true

        val granted = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Boolean> { cont ->
                val listener = object : Shizuku.OnRequestPermissionResultListener {
                    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                        if (requestCode == PERMISSION_REQUEST_CODE) {
                            Shizuku.removeRequestPermissionResultListener(this)
                            if (cont.isActive) {
                                cont.resume(grantResult == PackageManager.PERMISSION_GRANTED)
                            }
                        }
                    }
                }
                Shizuku.addRequestPermissionResultListener(listener)
                cont.invokeOnCancellation {
                    Shizuku.removeRequestPermissionResultListener(listener)
                }
                Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
            }
        }
        return granted == true
    }

    fun buildPrivilegedArgs(context: Context, config: TargetIntentConfig): Array<String>? {
        val pkg = config.packageName.trim()
        val activity = config.activityClass.trim()
        val action = config.action.trim()
        val dataUri = config.dataUri.trim()
        val category = config.category.trim()
        val mimeType = config.mimeType.trim()
        val extraTokens = if (config.extraArgs.isNotBlank()) {
            AdbCommandParser.tokenizeShellArgs(config.extraArgs.trim())
        } else {
            emptyList()
        }

        if (config.kind == TargetKind.QS_TILE) {
            if (pkg.isBlank() || activity.isBlank()) return null
            val resolvedClass = if (activity.startsWith(".")) "$pkg$activity" else activity
            return arrayOf("cmd", "statusbar", "click-tile", "$pkg/$resolvedClass")
        }

        if (pkg.isBlank() && action.isBlank() && dataUri.isBlank()) {
            return null
        }

        val args = mutableListOf("am", "start")
        if (pkg.isNotBlank() &&
            activity.isBlank() &&
            action.isBlank() &&
            dataUri.isBlank() &&
            category.isBlank() &&
            mimeType.isBlank() &&
            extraTokens.isEmpty()
        ) {
            val resolvedComponent = RemoteIntentLauncher.buildIntent(context, config)?.component
            if (resolvedComponent != null) {
                args.add("-n")
                args.add("${resolvedComponent.packageName}/${resolvedComponent.className}")
            } else {
                args.add("-a")
                args.add(Intent.ACTION_MAIN)
                args.add("-c")
                args.add(Intent.CATEGORY_LAUNCHER)
                args.add("-p")
                args.add(pkg)
            }
            return args.toTypedArray()
        }

        if (action.isNotBlank()) {
            args.add("-a")
            args.add(action)
        }
        if (dataUri.isNotBlank()) {
            args.add("-d")
            args.add(dataUri)
        }
        if (mimeType.isNotBlank()) {
            args.add("-t")
            args.add(mimeType)
        }
        if (category.isNotBlank()) {
            args.add("-c")
            args.add(category)
        }
        if (pkg.isNotBlank() && activity.isNotBlank()) {
            val resolvedClass = if (activity.startsWith(".")) "$pkg$activity" else activity
            args.add("-n")
            args.add("$pkg/$resolvedClass")
        } else if (pkg.isNotBlank()) {
            args.add("-p")
            args.add(pkg)
        }
        args.addAll(extraTokens)
        return args.toTypedArray()
    }

    suspend fun launchViaPrivilegedService(
        context: Context,
        config: TargetIntentConfig,
    ): RemoteIntentLauncher.LaunchOutcome {
        val args = buildPrivilegedArgs(context, config)
            ?: return RemoteIntentLauncher.LaunchOutcome.InvalidTarget

        if (!isBinderAlive() && getStatus(context) == BridgeStatus.NOT_INSTALLED) {
            return RemoteIntentLauncher.LaunchOutcome.ShizukuInstallRequired(
                packageName = config.packageName,
                activityClass = config.activityClass,
                isQsTile = config.kind == TargetKind.QS_TILE,
                hasShellExtras = config.extraArgs.isNotBlank(),
            )
        }

        val binderReady = awaitBinderAvailable(timeoutMs = 3_500L)
        if (!binderReady) {
            return when (getStatus(context)) {
                BridgeStatus.STUB_MISSING -> RemoteIntentLauncher.LaunchOutcome.SheveryStubMissing
                BridgeStatus.SERVICE_NOT_RUNNING ->
                    RemoteIntentLauncher.LaunchOutcome.SheveryServiceNotRunning
                BridgeStatus.NOT_INSTALLED -> RemoteIntentLauncher.LaunchOutcome.ShizukuInstallRequired(
                    packageName = config.packageName,
                    activityClass = config.activityClass,
                    isQsTile = config.kind == TargetKind.QS_TILE,
                    hasShellExtras = config.extraArgs.isNotBlank(),
                )
                else -> RemoteIntentLauncher.LaunchOutcome.PermissionDenied(
                    config.packageName,
                    config.activityClass,
                )
            }
        }

        if (!hasPermission()) {
            val granted = requestPermissionAndAwait()
            if (!granted) {
                return RemoteIntentLauncher.LaunchOutcome.SheveryPermissionRequired
            }
        }

        val userServiceArgs = Shizuku.UserServiceArgs(
            ComponentName(
                BuildConfig.APPLICATION_ID,
                PrivilegedLauncherUserService::class.java.name,
            ),
        )
            .daemon(false)
            .processNameSuffix("privileged_launcher")
            .tag("remote_launcher_shell")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)

        return withContext(Dispatchers.Main) {
            var lastFailure: String? = null
            for (attempt in 0..1) {
                var activeConnection: ServiceConnection? = null
                try {
                    val service = withTimeoutOrNull(5_000L) {
                        suspendCancellableCoroutine<IRemoteShellService?> { cont ->
                            val connection = object : ServiceConnection {
                                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                                    val resolved = if (binder != null && binder.pingBinder()) {
                                        IRemoteShellService.Stub.asInterface(binder)
                                    } else {
                                        null
                                    }
                                    if (cont.isActive) {
                                        cont.resume(resolved)
                                    }
                                }

                                override fun onServiceDisconnected(name: ComponentName?) {
                                    if (cont.isActive) {
                                        cont.resume(null)
                                    }
                                }
                            }
                            activeConnection = connection
                            Shizuku.bindUserService(userServiceArgs, connection)
                        }
                    }

                    if (service == null) {
                        lastFailure = "Timed out connecting to Shevery / Shizuku user service."
                        if (attempt == 0) {
                            delay(250L)
                            continue
                        }
                        return@withContext RemoteIntentLauncher.LaunchOutcome.Failed(lastFailure)
                    }

                    val errorOutput = withContext(Dispatchers.IO) {
                        service.runPrivilegedCommand(args)
                    }

                    if (errorOutput.isBlank()) {
                        if (config.kind == TargetKind.QS_TILE) {
                            // Give SystemUI time to bind the TileService and invoke startActivityAndCollapse
                            // before MainActivity removes its transparent window/task.
                            delay(450L)
                        }
                        return@withContext RemoteIntentLauncher.LaunchOutcome.Launched
                    } else if (
                        errorOutput.contains("does not exist", ignoreCase = true) ||
                        errorOutput.contains("unable to resolve", ignoreCase = true) ||
                        errorOutput.contains("ActivityNotFoundException", ignoreCase = true)
                    ) {
                        return@withContext if (
                            config.packageName.isNotBlank() &&
                            !RemoteIntentLauncher.isPackageInstalled(context, config.packageName)
                        ) {
                            RemoteIntentLauncher.LaunchOutcome.AppNotInstalled(config.packageName)
                        } else {
                            RemoteIntentLauncher.LaunchOutcome.ActivityNotFound(
                                config.packageName,
                                config.activityClass,
                            )
                        }
                    } else {
                        return@withContext RemoteIntentLauncher.LaunchOutcome.Failed(errorOutput)
                    }
                } catch (t: Throwable) {
                    lastFailure = t.localizedMessage
                    if (attempt == 0) {
                        delay(250L)
                        continue
                    }
                    return@withContext RemoteIntentLauncher.LaunchOutcome.Failed(lastFailure)
                } finally {
                    val conn = activeConnection
                    if (conn != null) {
                        runCatching { Shizuku.unbindUserService(userServiceArgs, conn, false) }
                    }
                }
            }
            RemoteIntentLauncher.LaunchOutcome.Failed(lastFailure)
        }
    }
}
