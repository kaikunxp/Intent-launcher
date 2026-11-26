package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.launcher.AdbCommandParser
import com.example.launcher.AppInspectionResult
import com.example.launcher.AppIntentEntry
import com.example.launcher.InstalledAppInfo
import com.example.launcher.IntentPreferencesRepository
import com.example.launcher.PackageActivityInfo
import com.example.launcher.ParsedAdbResult
import com.example.launcher.PreflightResult
import com.example.launcher.QsTileInfo
import com.example.launcher.RemoteIntentLauncher
import com.example.launcher.RemoteIntentLauncher.LaunchOutcome
import com.example.launcher.SheveryBridge
import com.example.launcher.ShizukuRequirementReason
import com.example.launcher.TargetIntentConfig
import com.example.launcher.TargetKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

enum class SelectorTab {
    APPS,
    QS_TILES,
    PRESETS,
    CUSTOM,
}

data class ShizukuHintPrompt(
    val targetLabel: String = "",
    val packageName: String = "",
    val reason: ShizukuRequirementReason = ShizukuRequirementReason.UNEXPORTED_ACTIVITY,
    val requiredPermission: String? = null,
    val bridgeStatus: SheveryBridge.BridgeStatus = SheveryBridge.BridgeStatus.NOT_INSTALLED,
)

data class RemoteLauncherUiState(
    val isInitializing: Boolean = true,
    val launchOutcome: LaunchOutcome = LaunchOutcome.Idle,
    val savedConfig: TargetIntentConfig = TargetIntentConfig.Default,
    val draftConfig: TargetIntentConfig = TargetIntentConfig.Default,
    val isTargetPackageInstalled: Boolean = false,
    val isGoogleTvInstalled: Boolean = false,
    val sheveryStatus: SheveryBridge.BridgeStatus = SheveryBridge.BridgeStatus.NOT_INSTALLED,
    val sheveryDiagnostic: String = "",
    val isLaunchingViaShevery: Boolean = false,
    val selectedTab: SelectorTab = SelectorTab.APPS,
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val isLoadingApps: Boolean = false,
    val appSearchQuery: String = "",
    val qsTiles: List<QsTileInfo> = emptyList(),
    val isLoadingTiles: Boolean = false,
    val tileSearchQuery: String = "",
    val presetPreflight: Map<String, PreflightResult> = emptyMap(),
    val pasteCommandText: String = "",
    val parsedAdbResult: ParsedAdbResult? = null,
    val inspectedAppResult: AppInspectionResult? = null,
    val isInspectingApp: Boolean = false,
    val shizukuHintPrompt: ShizukuHintPrompt? = null,
)

class RemoteLauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = IntentPreferencesRepository(application)

    private val _uiState = MutableStateFlow(RemoteLauncherUiState())
    val uiState: StateFlow<RemoteLauncherUiState> = _uiState.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        val app = getApplication<Application>()
        _uiState.update {
            it.copy(
                sheveryStatus = SheveryBridge.getStatus(app),
                sheveryDiagnostic = SheveryBridge.getDiagnosticLine(app),
            )
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        val app = getApplication<Application>()
        _uiState.update {
            it.copy(
                sheveryStatus = SheveryBridge.getStatus(app),
                sheveryDiagnostic = SheveryBridge.getDiagnosticLine(app),
            )
        }
    }

    init {
        runCatching {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
        }
    }

    override fun onCleared() {
        runCatching {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
        }
        super.onCleared()
    }

    fun initialize(forceConfigure: Boolean, onAutoLaunchSucceeded: () -> Unit) {
        viewModelScope.launch {
            val config = withContext(Dispatchers.IO) { repository.getConfig() }
            val app = getApplication<Application>()

            if (!forceConfigure && config.isConfigured && config.autoLaunchEnabled && config.isValid) {
                val outcome = executeConfiguredLaunch(app, config)
                if (outcome is LaunchOutcome.Launched) {
                    onAutoLaunchSucceeded()
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        isInitializing = false,
                        launchOutcome = outcome,
                        savedConfig = config,
                        draftConfig = config,
                        isTargetPackageInstalled = RemoteIntentLauncher.isPackageInstalled(
                            app,
                            config.packageName,
                        ),
                        isGoogleTvInstalled = RemoteIntentLauncher.isPackageInstalled(
                            app,
                            RemoteIntentLauncher.GOOGLE_TV_PACKAGE,
                        ),
                        sheveryStatus = SheveryBridge.getStatus(app),
                        sheveryDiagnostic = SheveryBridge.getDiagnosticLine(app),
                        shizukuHintPrompt = (outcome as? LaunchOutcome.ShizukuInstallRequired)
                            ?.toHintPrompt(app, config),
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isInitializing = false,
                        launchOutcome = LaunchOutcome.Idle,
                        savedConfig = config,
                        draftConfig = config,
                        isTargetPackageInstalled = RemoteIntentLauncher.isPackageInstalled(
                            app,
                            config.packageName,
                        ),
                        isGoogleTvInstalled = RemoteIntentLauncher.isPackageInstalled(
                            app,
                            RemoteIntentLauncher.GOOGLE_TV_PACKAGE,
                        ),
                        sheveryStatus = SheveryBridge.getStatus(app),
                        sheveryDiagnostic = SheveryBridge.getDiagnosticLine(app),
                        shizukuHintPrompt = null,
                    )
                }
            }

            loadDiscoveryData()
        }
    }

    fun dismissShizukuPrompt() {
        _uiState.update { it.copy(shizukuHintPrompt = null) }
    }

    fun showShizukuHintForLockedTarget(
        targetLabel: String,
        packageName: String,
        reason: ShizukuRequirementReason,
        requiredPermission: String? = null,
    ) {
        val app = getApplication<Application>()
        val status = SheveryBridge.getStatus(app)
        _uiState.update {
            it.copy(
                sheveryStatus = status,
                sheveryDiagnostic = SheveryBridge.getDiagnosticLine(app),
                shizukuHintPrompt = ShizukuHintPrompt(
                    targetLabel = targetLabel,
                    packageName = packageName,
                    reason = reason,
                    requiredPermission = requiredPermission,
                    bridgeStatus = status,
                ),
            )
        }
    }

    fun requestSheveryPermissionNow(onGranted: () -> Unit = {}) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            val binderReady = SheveryBridge.awaitBinderAvailable(timeoutMs = 2_000L)
            if (binderReady) {
                val granted = SheveryBridge.requestPermissionAndAwait(timeoutMs = 30_000L)
                _uiState.update {
                    it.copy(
                        sheveryStatus = SheveryBridge.getStatus(app),
                        sheveryDiagnostic = SheveryBridge.getDiagnosticLine(app),
                    )
                }
                if (granted) {
                    onGranted()
                }
            } else {
                _uiState.update {
                    it.copy(
                        launchOutcome = when (SheveryBridge.getStatus(app)) {
                            SheveryBridge.BridgeStatus.STUB_MISSING -> LaunchOutcome.SheveryStubMissing
                            else -> LaunchOutcome.SheveryServiceNotRunning
                        },
                        sheveryStatus = SheveryBridge.getStatus(app),
                        sheveryDiagnostic = SheveryBridge.getDiagnosticLine(app),
                    )
                }
            }
        }
    }

    fun selectTab(tab: SelectorTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab == SelectorTab.APPS) {
            loadInstalledAppsIfNeeded()
        } else if (tab == SelectorTab.QS_TILES) {
            loadQsTilesIfNeeded()
        }
    }

    fun openPasteBoxTab() {
        _uiState.update { it.copy(selectedTab = SelectorTab.CUSTOM) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(appSearchQuery = query) }
    }

    fun updateTileSearchQuery(query: String) {
        _uiState.update { it.copy(tileSearchQuery = query) }
    }

    fun updatePasteCommandText(text: String) {
        val draft = _uiState.value.draftConfig
        val parsed = if (text.isBlank()) {
            null
        } else {
            AdbCommandParser.parse(
                rawInput = text,
                currentAutoLaunch = draft.autoLaunchEnabled,
                currentPreferPrivileged = draft.preferPrivilegedLaunch,
            )
        }
        _uiState.update {
            it.copy(
                pasteCommandText = text,
                parsedAdbResult = parsed,
            )
        }
    }

    fun applyParsedCommand(
        launchImmediately: Boolean,
        onApplied: () -> Unit = {},
        onLaunchSucceeded: () -> Unit = {},
    ) {
        val text = _uiState.value.pasteCommandText
        val draft = _uiState.value.draftConfig
        val parsed = AdbCommandParser.parse(
            rawInput = text,
            currentAutoLaunch = draft.autoLaunchEnabled,
            currentPreferPrivileged = draft.preferPrivilegedLaunch,
        )
        _uiState.update { it.copy(parsedAdbResult = parsed) }
        val config = parsed.config ?: return
        if (launchImmediately) {
            saveAndLaunchConfig(config, onLaunchSucceeded)
        } else {
            applyDraftConfig(config)
            onApplied()
        }
    }

    fun applyDraftConfig(config: TargetIntentConfig) {
        val app = getApplication<Application>()
        _uiState.update {
            it.copy(
                draftConfig = config.copy(isConfigured = true),
                launchOutcome = LaunchOutcome.Idle,
                shizukuHintPrompt = null,
                isTargetPackageInstalled = RemoteIntentLauncher.isPackageInstalled(
                    app,
                    config.packageName,
                ),
            )
        }
    }

    fun updateDraftFields(
        label: String? = null,
        packageName: String? = null,
        activityClass: String? = null,
        action: String? = null,
        dataUri: String? = null,
        extraArgs: String? = null,
        autoLaunchEnabled: Boolean? = null,
        preferPrivilegedLaunch: Boolean? = null,
        kind: TargetKind? = null,
    ) {
        val current = _uiState.value.draftConfig
        val updated = current.copy(
            label = label ?: current.label,
            packageName = packageName ?: current.packageName,
            activityClass = activityClass ?: current.activityClass,
            action = action ?: current.action,
            dataUri = dataUri ?: current.dataUri,
            extraArgs = extraArgs ?: current.extraArgs,
            autoLaunchEnabled = autoLaunchEnabled ?: current.autoLaunchEnabled,
            preferPrivilegedLaunch = preferPrivilegedLaunch ?: current.preferPrivilegedLaunch,
            kind = kind ?: current.kind,
            isConfigured = true,
        )
        applyDraftConfig(updated)
    }

    fun selectInstalledApp(appInfo: InstalledAppInfo) {
        applyDraftConfig(
            _uiState.value.draftConfig.copy(
                label = appInfo.label,
                packageName = appInfo.packageName,
                activityClass = appInfo.launcherActivity,
                action = "",
                dataUri = "",
                category = "",
                mimeType = "",
                extraArgs = "",
                kind = TargetKind.ACTIVITY,
            ),
        )
    }

    fun selectQsTile(tileInfo: QsTileInfo) {
        applyDraftConfig(
            _uiState.value.draftConfig.copy(
                label = "${tileInfo.label} (QS Tile)",
                packageName = tileInfo.packageName,
                activityClass = tileInfo.serviceClass,
                action = "",
                dataUri = "",
                category = "",
                mimeType = "",
                extraArgs = "",
                preferPrivilegedLaunch = true,
                kind = TargetKind.QS_TILE,
            ),
        )
    }

    fun inspectPackageDetails(label: String, packageName: String) {
        val pkg = packageName.trim()
        if (pkg.isBlank()) return
        _uiState.update { it.copy(isInspectingApp = true) }
        viewModelScope.launch {
            val app = getApplication<Application>()
            val result = withContext(Dispatchers.IO) {
                RemoteIntentLauncher.inspectAppPackage(app, label, pkg)
            }
            _uiState.update {
                it.copy(
                    inspectedAppResult = result,
                    isInspectingApp = false,
                )
            }
        }
    }

    fun dismissActivityInspector() {
        _uiState.update {
            it.copy(
                inspectedAppResult = null,
                isInspectingApp = false,
            )
        }
    }

    fun selectAppIntentEntry(
        entry: AppIntentEntry,
        launchImmediately: Boolean,
        onLaunchSucceeded: () -> Unit = {},
    ) {
        val appLabel = _uiState.value.inspectedAppResult?.appLabel.orEmpty()
        val chosenLabel = if (appLabel.isNotBlank() && entry.title != appLabel) {
            "$appLabel (${entry.title})"
        } else {
            entry.title
        }
        val config = _uiState.value.draftConfig.copy(
            label = chosenLabel,
            packageName = entry.packageName,
            activityClass = entry.componentClass,
            action = entry.action,
            dataUri = entry.dataUri,
            category = entry.category,
            mimeType = entry.mimeType,
            extraArgs = "",
            preferPrivilegedLaunch = if (entry.kind == TargetKind.QS_TILE) {
                true
            } else {
                _uiState.value.draftConfig.preferPrivilegedLaunch
            },
            kind = entry.kind,
            isConfigured = true,
        )
        dismissActivityInspector()
        if (launchImmediately) {
            saveAndLaunchConfig(config, onLaunchSucceeded)
        } else {
            applyDraftConfig(config)
        }
    }

    fun selectInspectedActivity(
        activity: PackageActivityInfo,
        launchImmediately: Boolean,
        onLaunchSucceeded: () -> Unit = {},
    ) {
        val pkg = _uiState.value.inspectedAppResult?.packageName ?: return
        val appLabel = _uiState.value.inspectedAppResult?.appLabel.orEmpty()
        val chosenLabel = if (appLabel.isNotBlank()) {
            "$appLabel (${activity.shortName})"
        } else {
            activity.label
        }
        val config = _uiState.value.draftConfig.copy(
            label = chosenLabel,
            packageName = pkg,
            activityClass = activity.className,
            action = "",
            dataUri = "",
            category = "",
            mimeType = "",
            extraArgs = "",
            kind = TargetKind.ACTIVITY,
            isConfigured = true,
        )
        dismissActivityInspector()
        if (launchImmediately) {
            saveAndLaunchConfig(config, onLaunchSucceeded)
        } else {
            applyDraftConfig(config)
        }
    }

    fun saveOnly(onSaved: () -> Unit) {
        val draft = _uiState.value.draftConfig.copy(isConfigured = true)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.saveConfig(draft) }
            _uiState.update { it.copy(savedConfig = draft, draftConfig = draft) }
            onSaved()
        }
    }

    fun saveAndLaunch(onLaunchSucceeded: () -> Unit) {
        saveAndLaunchConfig(_uiState.value.draftConfig, onLaunchSucceeded)
    }

    fun saveAndLaunchConfig(config: TargetIntentConfig, onLaunchSucceeded: () -> Unit) {
        val app = getApplication<Application>()
        val configured = config.copy(isConfigured = true)
        viewModelScope.launch {
            _uiState.update { it.copy(isLaunchingViaShevery = true, draftConfig = configured) }
            withContext(Dispatchers.IO) { repository.saveConfig(configured) }
            val outcome = executeConfiguredLaunch(app, configured)
            if (outcome is LaunchOutcome.Launched) {
                    _uiState.update {
                    it.copy(
                        savedConfig = configured,
                        launchOutcome = outcome,
                        isLaunchingViaShevery = false,
                        sheveryStatus = SheveryBridge.getStatus(app),
                        sheveryDiagnostic = SheveryBridge.getDiagnosticLine(app),
                        shizukuHintPrompt = null,
                    )
                }
                onLaunchSucceeded()
            } else {
                _uiState.update {
                    it.copy(
                        savedConfig = configured,
                        launchOutcome = outcome,
                        isLaunchingViaShevery = false,
                        isTargetPackageInstalled = RemoteIntentLauncher.isPackageInstalled(
                            app,
                            configured.packageName,
                        ),
                        sheveryStatus = SheveryBridge.getStatus(app),
                        sheveryDiagnostic = SheveryBridge.getDiagnosticLine(app),
                        shizukuHintPrompt = (outcome as? LaunchOutcome.ShizukuInstallRequired)
                            ?.toHintPrompt(app, configured),
                    )
                }
            }
        }
    }

    fun launchViaSheveryNow(onLaunchSucceeded: () -> Unit) {
        val draft = _uiState.value.draftConfig
        val app = getApplication<Application>()
        viewModelScope.launch {
            _uiState.update { it.copy(isLaunchingViaShevery = true) }
            val outcome = SheveryBridge.launchViaPrivilegedService(app, draft)
            if (outcome is LaunchOutcome.Launched) {
                _uiState.update {
                    it.copy(
                        launchOutcome = outcome,
                        isLaunchingViaShevery = false,
                        sheveryStatus = SheveryBridge.getStatus(app),
                        sheveryDiagnostic = SheveryBridge.getDiagnosticLine(app),
                        shizukuHintPrompt = null,
                    )
                }
                onLaunchSucceeded()
            } else {
                _uiState.update {
                    it.copy(
                        launchOutcome = outcome,
                        isLaunchingViaShevery = false,
                        sheveryStatus = SheveryBridge.getStatus(app),
                        sheveryDiagnostic = SheveryBridge.getDiagnosticLine(app),
                        shizukuHintPrompt = (outcome as? LaunchOutcome.ShizukuInstallRequired)
                            ?.toHintPrompt(app, draft),
                    )
                }
            }
        }
    }

    fun refreshInstallStatus() {
        val app = getApplication<Application>()
        val draftPkg = _uiState.value.draftConfig.packageName
        _uiState.update {
            it.copy(
                isTargetPackageInstalled = RemoteIntentLauncher.isPackageInstalled(app, draftPkg),
                isGoogleTvInstalled = RemoteIntentLauncher.isPackageInstalled(
                    app,
                    RemoteIntentLauncher.GOOGLE_TV_PACKAGE,
                ),
                sheveryStatus = SheveryBridge.getStatus(app),
                sheveryDiagnostic = SheveryBridge.getDiagnosticLine(app),
            )
        }
        loadPresetPreflight()
        if (_uiState.value.selectedTab == SelectorTab.APPS && _uiState.value.installedApps.isEmpty()) {
            loadInstalledAppsIfNeeded()
        }
    }

    private suspend fun executeConfiguredLaunch(
        app: Application,
        config: TargetIntentConfig,
    ): LaunchOutcome {
        val hasShellExtras = config.extraArgs.isNotBlank()
        if (!hasShellExtras) {
            val directOutcome = RemoteIntentLauncher.launchTarget(app, config)
            if (directOutcome is LaunchOutcome.Launched) {
                return directOutcome
            }

            val blockedNatively = when (directOutcome) {
                is LaunchOutcome.PermissionDenied -> true
                is LaunchOutcome.ActivityNotFound -> RemoteIntentLauncher.isActivityDeclared(
                    app,
                    config.packageName,
                    config.activityClass,
                )
                else -> false
            }
            if (!blockedNatively) {
                return directOutcome
            }

            if (SheveryBridge.getStatus(app) == SheveryBridge.BridgeStatus.NOT_INSTALLED) {
                return LaunchOutcome.ShizukuInstallRequired(
                    packageName = config.packageName,
                    activityClass = config.activityClass,
                    isQsTile = config.kind == TargetKind.QS_TILE,
                    hasShellExtras = false,
                )
            }
            if (!config.preferPrivilegedLaunch) {
                return directOutcome
            }
            return SheveryBridge.launchViaPrivilegedService(app, config)
        }

        if (SheveryBridge.getStatus(app) == SheveryBridge.BridgeStatus.NOT_INSTALLED) {
            return LaunchOutcome.ShizukuInstallRequired(
                packageName = config.packageName,
                activityClass = config.activityClass,
                isQsTile = config.kind == TargetKind.QS_TILE,
                hasShellExtras = true,
            )
        }
        if (!config.preferPrivilegedLaunch) {
            return LaunchOutcome.PermissionDenied(config.packageName, config.activityClass)
        }
        return SheveryBridge.launchViaPrivilegedService(app, config)
    }

    private fun LaunchOutcome.ShizukuInstallRequired.toHintPrompt(
        app: Application,
        config: TargetIntentConfig,
    ): ShizukuHintPrompt {
        val preflight = RemoteIntentLauncher.checkNativeCompatibility(app, config)
        val reason = when {
            isQsTile -> ShizukuRequirementReason.QUICK_SETTINGS_TILE
            hasShellExtras -> ShizukuRequirementReason.SHELL_EXTRA_FLAGS
            preflight.requiresShizuku -> preflight.reason
            else -> ShizukuRequirementReason.UNEXPORTED_ACTIVITY
        }
        return ShizukuHintPrompt(
            targetLabel = config.label.ifBlank { config.packageName },
            packageName = packageName,
            reason = reason,
            requiredPermission = preflight.requiredPermission,
            bridgeStatus = SheveryBridge.getStatus(app),
        )
    }

    private fun loadDiscoveryData() {
        loadPresetPreflight()
        loadInstalledAppsIfNeeded()
        loadQsTilesIfNeeded()
    }

    private fun loadPresetPreflight() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val preflightMap = withContext(Dispatchers.IO) {
                TargetIntentConfig.Presets.associate { preset ->
                    preset.id to RemoteIntentLauncher.checkNativeCompatibility(app, preset.config)
                }
            }
            _uiState.update { it.copy(presetPreflight = preflightMap) }
        }
    }

    private fun loadQsTilesIfNeeded() {
        if (_uiState.value.isLoadingTiles || _uiState.value.qsTiles.isNotEmpty()) return
        _uiState.update { it.copy(isLoadingTiles = true) }
        viewModelScope.launch {
            val app = getApplication<Application>()
            val tiles = withContext(Dispatchers.IO) {
                RemoteIntentLauncher.queryQuickSettingsTiles(app)
            }
            _uiState.update {
                it.copy(
                    qsTiles = tiles,
                    isLoadingTiles = false,
                )
            }
        }
    }

    private fun loadInstalledAppsIfNeeded() {
        if (_uiState.value.isLoadingApps || _uiState.value.installedApps.isNotEmpty()) return
        _uiState.update { it.copy(isLoadingApps = true) }
        viewModelScope.launch {
            val app = getApplication<Application>()
            val apps = withContext(Dispatchers.IO) {
                RemoteIntentLauncher.queryLaunchableApps(app)
            }
            _uiState.update {
                it.copy(
                    installedApps = apps,
                    isLoadingApps = false,
                )
            }
        }
    }
}
