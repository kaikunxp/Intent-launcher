package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.launcher.InstalledAppInfo
import com.example.launcher.IntentPreferencesRepository
import com.example.launcher.PackageActivityInfo
import com.example.launcher.QsTileInfo
import com.example.launcher.RemoteActivityCandidate
import com.example.launcher.RemoteIntentLauncher
import com.example.launcher.RemoteIntentLauncher.LaunchOutcome
import com.example.launcher.SheveryBridge
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
    PRESETS,
    QS_TILES,
    APPS,
    CUSTOM,
}

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
    val selectedTab: SelectorTab = SelectorTab.PRESETS,
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val isLoadingApps: Boolean = false,
    val appSearchQuery: String = "",
    val qsTiles: List<QsTileInfo> = emptyList(),
    val isLoadingTiles: Boolean = false,
    val tileSearchQuery: String = "",
    val discoveredRemoteActivities: List<RemoteActivityCandidate> = emptyList(),
    val inspectedAppLabel: String? = null,
    val inspectedPackageName: String? = null,
    val inspectedActivities: List<PackageActivityInfo> = emptyList(),
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

            if (!forceConfigure && config.autoLaunchEnabled && config.isValid) {
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
                    )
                }
            }
            loadDiscoveryData()
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

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(appSearchQuery = query) }
    }

    fun updateTileSearchQuery(query: String) {
        _uiState.update { it.copy(tileSearchQuery = query) }
    }

    fun applyDraftConfig(config: TargetIntentConfig) {
        val app = getApplication<Application>()
        _uiState.update {
            it.copy(
                draftConfig = config,
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
            autoLaunchEnabled = autoLaunchEnabled ?: current.autoLaunchEnabled,
            preferPrivilegedLaunch = preferPrivilegedLaunch ?: current.preferPrivilegedLaunch,
            kind = kind ?: current.kind,
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
                preferPrivilegedLaunch = true,
                kind = TargetKind.QS_TILE,
            ),
        )
    }

    fun selectRemoteCandidate(candidate: RemoteActivityCandidate) {
        applyDraftConfig(
            _uiState.value.draftConfig.copy(
                label = "${candidate.appLabel} (${candidate.shortName})",
                packageName = candidate.packageName,
                activityClass = candidate.className,
                action = "",
                dataUri = "",
                preferPrivilegedLaunch = true,
                kind = TargetKind.ACTIVITY,
            ),
        )
    }

    fun inspectPackageActivities(label: String, packageName: String) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val activities = withContext(Dispatchers.IO) {
                RemoteIntentLauncher.queryPackageActivities(app, packageName)
            }
            _uiState.update {
                it.copy(
                    inspectedAppLabel = label,
                    inspectedPackageName = packageName,
                    inspectedActivities = activities,
                )
            }
        }
    }

    fun dismissActivityInspector() {
        _uiState.update {
            it.copy(
                inspectedAppLabel = null,
                inspectedPackageName = null,
                inspectedActivities = emptyList(),
            )
        }
    }

    fun selectInspectedActivity(activity: PackageActivityInfo) {
        val pkg = _uiState.value.inspectedPackageName ?: return
        val appLabel = _uiState.value.inspectedAppLabel.orEmpty()
        val chosenLabel = if (appLabel.isNotBlank()) {
            "$appLabel (${activity.shortName})"
        } else {
            activity.label
        }
        applyDraftConfig(
            _uiState.value.draftConfig.copy(
                label = chosenLabel,
                packageName = pkg,
                activityClass = activity.className,
                action = "",
                dataUri = "",
                kind = TargetKind.ACTIVITY,
            ),
        )
        dismissActivityInspector()
    }

    fun saveOnly(onSaved: () -> Unit) {
        val draft = _uiState.value.draftConfig
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.saveConfig(draft) }
            _uiState.update { it.copy(savedConfig = draft) }
            onSaved()
        }
    }

    fun saveAndLaunch(onLaunchSucceeded: () -> Unit) {
        saveAndLaunchConfig(_uiState.value.draftConfig, onLaunchSucceeded)
    }

    fun saveAndLaunchConfig(config: TargetIntentConfig, onLaunchSucceeded: () -> Unit) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            _uiState.update { it.copy(isLaunchingViaShevery = true, draftConfig = config) }
            withContext(Dispatchers.IO) { repository.saveConfig(config) }
            val outcome = executeConfiguredLaunch(app, config)
            if (outcome is LaunchOutcome.Launched) {
                _uiState.update {
                    it.copy(
                        savedConfig = config,
                        launchOutcome = outcome,
                        isLaunchingViaShevery = false,
                        sheveryStatus = SheveryBridge.getStatus(app),
                        sheveryDiagnostic = SheveryBridge.getDiagnosticLine(app),
                    )
                }
                onLaunchSucceeded()
            } else {
                _uiState.update {
                    it.copy(
                        savedConfig = config,
                        launchOutcome = outcome,
                        isLaunchingViaShevery = false,
                        isTargetPackageInstalled = RemoteIntentLauncher.isPackageInstalled(
                            app,
                            config.packageName,
                        ),
                        sheveryStatus = SheveryBridge.getStatus(app),
                        sheveryDiagnostic = SheveryBridge.getDiagnosticLine(app),
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
        if (_uiState.value.selectedTab == SelectorTab.APPS && _uiState.value.installedApps.isEmpty()) {
            loadInstalledAppsIfNeeded()
        }
    }

    private suspend fun executeConfiguredLaunch(
        app: Application,
        config: TargetIntentConfig,
    ): LaunchOutcome {
        val directOutcome = RemoteIntentLauncher.launchTarget(app, config)
        if (directOutcome is LaunchOutcome.Launched) {
            return directOutcome
        }
        val shouldFallbackToShevery = config.preferPrivilegedLaunch &&
            (directOutcome is LaunchOutcome.PermissionDenied || directOutcome is LaunchOutcome.ActivityNotFound)
        if (shouldFallbackToShevery) {
            return SheveryBridge.launchViaPrivilegedService(app, config)
        }
        return directOutcome
    }

    private fun loadDiscoveryData() {
        loadQsTilesIfNeeded()
        loadRemoteActivitiesOnDevice()
        loadInstalledAppsIfNeeded()
    }

    private fun loadRemoteActivitiesOnDevice() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val candidates = withContext(Dispatchers.IO) {
                RemoteIntentLauncher.findRemoteActivitiesOnDevice(app)
            }
            _uiState.update { it.copy(discoveredRemoteActivities = candidates) }
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
