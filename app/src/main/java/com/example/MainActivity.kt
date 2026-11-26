package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: RemoteLauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val forceConfigure = intent?.action == RemoteIntentLauncher.ACTION_CONFIGURE_INTENT
        if (savedInstanceState == null) {
            viewModel.initialize(
                forceConfigure = forceConfigure,
                onAutoLaunchSucceeded = { finishAndRemoveTask() },
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
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onTileSearchQueryChange = viewModel::updateTileSearchQuery,
                    onSelectPreset = { preset -> viewModel.applyDraftConfig(preset.config) },
                    onSelectQsTile = viewModel::selectQsTile,
                    onSelectApp = viewModel::selectInstalledApp,
                    onBrowseActivities = viewModel::inspectPackageActivities,
                    onSelectActivity = { activity ->
                        viewModel.selectInspectedActivity(activity)
                        if (!activity.exported && !uiState.draftConfig.preferPrivilegedLaunch) {
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
                    onUpdateKind = { viewModel.updateDraftFields(kind = it) },
                    onToggleAutoLaunch = { viewModel.updateDraftFields(autoLaunchEnabled = it) },
                    onTogglePrivilegedLaunch = {
                        viewModel.updateDraftFields(preferPrivilegedLaunch = it)
                    },
                    onSaveAndLaunch = {
                        viewModel.saveAndLaunch(
                            onLaunchSucceeded = { finishAndRemoveTask() },
                        )
                    },
                    onSaveAndLaunchConfig = { config ->
                        viewModel.saveAndLaunchConfig(
                            config = config,
                            onLaunchSucceeded = { finishAndRemoveTask() },
                        )
                    },
                    onSaveOnly = {
                        viewModel.saveOnly {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(getString(R.string.snackbar_saved))
                            }
                        }
                    },
                    onLaunchViaShevery = {
                        viewModel.launchViaSheveryNow(
                            onLaunchSucceeded = { finishAndRemoveTask() },
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
        val forceConfigure = intent.action == RemoteIntentLauncher.ACTION_CONFIGURE_INTENT
        viewModel.initialize(
            forceConfigure = forceConfigure,
            onAutoLaunchSucceeded = { finishAndRemoveTask() },
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshInstallStatus()
    }
}
