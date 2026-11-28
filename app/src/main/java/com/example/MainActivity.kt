package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.launcher.RemoteIntentLauncher
import com.example.launcher.SheveryBridge
import com.example.ui.RemoteLauncherScreen
import com.example.ui.RemoteLauncherViewModel
import com.example.ui.theme.TvRemoteLauncherTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
    private val viewModel: RemoteLauncherViewModel by viewModels()
    private val pausedSeq = MutableStateFlow(0L)
    private val focusLostSeq = MutableStateFlow(0L)
    private var hasHadWindowFocus = false

    private val tileLaunchObserver = object : SheveryBridge.LaunchProgressObserver {
        override fun snapshotYieldedSeq(): Long = pausedSeq.value + focusLostSeq.value

        override suspend fun awaitYieldedSince(baselineSeq: Long, timeoutMs: Long): Boolean =
            withTimeoutOrNull(timeoutMs) {
                combine(pausedSeq, focusLostSeq) { paused, focusLost -> paused + focusLost }
                    .first { it > baselineSeq }
                true
            } ?: false

        override fun snapshotPausedSeq(): Long = pausedSeq.value

        override suspend fun awaitPausedSince(baselineSeq: Long, timeoutMs: Long): Boolean =
            withTimeoutOrNull(timeoutMs) {
                pausedSeq.first { it > baselineSeq }
                true
            } ?: false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.decorView.setBackgroundColor(Color.TRANSPARENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }

        val forceConfigure = isConfigureIntent(intent)
        if (savedInstanceState == null) {
            viewModel.initialize(
                forceConfigure = forceConfigure,
                observer = tileLaunchObserver,
                onAutoLaunchSucceeded = ::finishSilently,
            )
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            TvRemoteLauncherTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()

                RemoteLauncherScreen(
                    uiState = uiState,
                    snackbarHostState = snackbarHostState,
                    onSelectTab = viewModel::selectTab,
                    onOpenPasteBox = viewModel::openPasteBoxTab,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onTileSearchQueryChange = viewModel::updateTileSearchQuery,
                    onPasteTextChange = viewModel::updatePasteCommandText,
                    onPasteFromClipboard = {
                        val clipboard = getSystemService(ClipboardManager::class.java)
                        val clipText = clipboard?.primaryClip
                            ?.takeIf { it.itemCount > 0 }
                            ?.getItemAt(0)
                            ?.coerceToText(this)
                            ?.toString()
                            .orEmpty()
                        if (clipText.isBlank()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(getString(R.string.snackbar_clipboard_empty))
                            }
                        } else {
                            viewModel.updatePasteCommandText(clipText)
                        }
                    },
                    onApplyParsedCommand = {
                        viewModel.applyParsedCommand(
                            launchImmediately = false,
                            onApplied = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        getString(R.string.snackbar_parsed_applied),
                                    )
                                }
                            },
                        )
                    },
                    onRunParsedCommand = {
                        viewModel.applyParsedCommand(
                            launchImmediately = true,
                            observer = tileLaunchObserver,
                            onLaunchSucceeded = ::finishSilently,
                        )
                    },
                    onSelectPreset = { preset -> viewModel.applyDraftConfig(preset.config) },
                    onSelectQsTile = viewModel::selectQsTile,
                    onSelectApp = viewModel::selectInstalledApp,
                    onBrowseActivities = viewModel::inspectPackageDetails,
                    onSelectIntentEntry = { entry, runNow ->
                        viewModel.selectAppIntentEntry(
                            entry = entry,
                            launchImmediately = runNow,
                            observer = tileLaunchObserver,
                            onLaunchSucceeded = ::finishSilently,
                        )
                        if (!runNow && !entry.exported && !uiState.draftConfig.preferPrivilegedLaunch) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    getString(R.string.snackbar_internal_activity_warning),
                                )
                            }
                        }
                    },
                    onSelectActivity = { activity, runNow ->
                        viewModel.selectInspectedActivity(
                            activity = activity,
                            launchImmediately = runNow,
                            observer = tileLaunchObserver,
                            onLaunchSucceeded = ::finishSilently,
                        )
                        if (!runNow && !activity.exported && !uiState.draftConfig.preferPrivilegedLaunch) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    getString(R.string.snackbar_internal_activity_warning),
                                )
                            }
                        }
                    },
                    onDismissActivitiesSheet = viewModel::dismissActivityInspector,
                    onUpdateLabel = { viewModel.updateDraftFields(label = it) },
                    onUpdatePackage = { viewModel.updateDraftFields(packageName = it) },
                    onUpdateActivity = { viewModel.updateDraftFields(activityClass = it) },
                    onUpdateAction = { viewModel.updateDraftFields(action = it) },
                    onUpdateDataUri = { viewModel.updateDraftFields(dataUri = it) },
                    onUpdateExtraArgs = { viewModel.updateDraftFields(extraArgs = it) },
                    onUpdateKind = { viewModel.updateDraftFields(kind = it) },
                    onToggleAutoLaunch = { viewModel.updateDraftFields(autoLaunchEnabled = it) },
                    onTogglePrivilegedLaunch = {
                        viewModel.updateDraftFields(preferPrivilegedLaunch = it)
                    },
                    onToggleRetryTileIfNoScreen = {
                        viewModel.updateDraftFields(retryTileIfNoScreen = it)
                    },
                    onSaveAndLaunch = {
                        viewModel.saveAndLaunch(
                            observer = tileLaunchObserver,
                            onLaunchSucceeded = ::finishSilently,
                        )
                    },
                    onSaveOnly = {
                        viewModel.saveOnly {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(getString(R.string.snackbar_saved))
                            }
                        }
                    },
                    onOpenKeyRemapper = {
                        viewModel.openKeyRemapper(
                            onFallbackUsed = { fallbackLabel ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        getString(R.string.snackbar_key_remapper_fallback, fallbackLabel),
                                    )
                                }
                            },
                            onUnavailable = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        getString(R.string.snackbar_key_remapper_unavailable),
                                    )
                                }
                            },
                        )
                    },
                    onOpenFallbackKeyRemapper = {
                        viewModel.openFallbackKeyRemapper {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    getString(R.string.snackbar_key_remapper_unavailable),
                                )
                            }
                        }
                    },
                    onRequestSheveryPermission = {
                        viewModel.requestSheveryPermissionNow {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    getString(R.string.snackbar_shevery_granted),
                                )
                            }
                        }
                    },
                    onLaunchViaShevery = {
                        viewModel.launchViaSheveryNow(
                            observer = tileLaunchObserver,
                            onLaunchSucceeded = ::finishSilently,
                        )
                    },
                    onOpenShevery = {
                        if (!SheveryBridge.openSheveryOrShizuku(this)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    getString(R.string.snackbar_shevery_launch_failed),
                                )
                            }
                        }
                    },
                    onInstallShizuku = {
                        if (!SheveryBridge.openShizukuInstall(this)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    getString(R.string.snackbar_play_store_unavailable),
                                )
                            }
                        }
                    },
                    onGetShevery = {
                        if (!SheveryBridge.openSheveryReleases(this)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    getString(R.string.snackbar_browser_unavailable),
                                )
                            }
                        }
                    },
                    onLockedTargetClick = viewModel::showShizukuHintForLockedTarget,
                    onDismissShizukuPrompt = viewModel::dismissShizukuPrompt,
                    onOpenGoogleTv = {
                        if (RemoteIntentLauncher.launchGoogleTvApp(this)) {
                            finishAndRemoveTask()
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    getString(R.string.snackbar_google_tv_launch_failed),
                                )
                            }
                        }
                    },
                    onOpenPlayStore = { packageName ->
                        if (!RemoteIntentLauncher.openPackageInPlayStore(this, packageName)) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    getString(R.string.snackbar_play_store_unavailable),
                                )
                            }
                        }
                    },
                    onCopyComponent = {
                        val clipboard = getSystemService(ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(
                            ClipData.newPlainText(
                                getString(R.string.label_active_target),
                                uiState.draftConfig.displayTarget,
                            ),
                        )
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(getString(R.string.snackbar_copied))
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val forceConfigure = isConfigureIntent(intent)
        viewModel.initialize(
            forceConfigure = forceConfigure,
            observer = tileLaunchObserver,
            onAutoLaunchSucceeded = ::finishSilently,
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshInstallStatus()
    }

    override fun onPause() {
        pausedSeq.value += 1L
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hasHadWindowFocus = true
        } else if (hasHadWindowFocus) {
            focusLostSeq.value += 1L
        }
    }

    private fun isConfigureIntent(intent: Intent?): Boolean {
        val action = intent?.action ?: return false
        return action == RemoteIntentLauncher.ACTION_CONFIGURE_INTENT ||
            action == Intent.ACTION_APPLICATION_PREFERENCES
    }

    private fun finishSilently() {
        finishAndRemoveTask()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
