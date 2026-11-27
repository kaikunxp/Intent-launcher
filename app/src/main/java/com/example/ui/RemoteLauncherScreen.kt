package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.launcher.AppIntentEntry
import com.example.launcher.InstalledAppInfo
import com.example.launcher.IntentPreset
import com.example.launcher.PackageActivityInfo
import com.example.launcher.PreflightResult
import com.example.launcher.QsTileInfo
import com.example.launcher.RemoteIntentLauncher.LaunchOutcome
import com.example.launcher.SheveryBridge
import com.example.launcher.SheveryBridge.BridgeStatus
import com.example.launcher.ShizukuRequirementReason
import com.example.launcher.TargetIntentConfig
import com.example.launcher.TargetKind
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOk

@Composable
fun RemoteLauncherScreen(
    uiState: RemoteLauncherUiState,
    snackbarHostState: SnackbarHostState,
    onSelectTab: (SelectorTab) -> Unit,
    onOpenPasteBox: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTileSearchQueryChange: (String) -> Unit,
    onPasteTextChange: (String) -> Unit,
    onPasteFromClipboard: () -> Unit,
    onApplyParsedCommand: () -> Unit,
    onRunParsedCommand: () -> Unit,
    onSelectPreset: (IntentPreset) -> Unit,
    onSelectQsTile: (QsTileInfo) -> Unit,
    onSelectApp: (InstalledAppInfo) -> Unit,
    onBrowseActivities: (String, String) -> Unit,
    onSelectIntentEntry: (AppIntentEntry, Boolean) -> Unit,
    onSelectActivity: (PackageActivityInfo, Boolean) -> Unit,
    onDismissActivitiesSheet: () -> Unit,
    onUpdateLabel: (String) -> Unit,
    onUpdatePackage: (String) -> Unit,
    onUpdateActivity: (String) -> Unit,
    onUpdateAction: (String) -> Unit,
    onUpdateDataUri: (String) -> Unit,
    onUpdateExtraArgs: (String) -> Unit,
    onUpdateKind: (TargetKind) -> Unit,
    onToggleAutoLaunch: (Boolean) -> Unit,
    onTogglePrivilegedLaunch: (Boolean) -> Unit,
    onSaveAndLaunch: () -> Unit,
    onSaveOnly: () -> Unit,
    onRequestSheveryPermission: () -> Unit,
    onLaunchViaShevery: () -> Unit,
    onOpenShevery: () -> Unit,
    onInstallShizuku: () -> Unit,
    onGetShevery: () -> Unit,
    onLockedTargetClick: (String, String, ShizukuRequirementReason, String?) -> Unit,
    onDismissShizukuPrompt: () -> Unit,
    onOpenGoogleTv: () -> Unit,
    onOpenPlayStore: (String) -> Unit,
    onCopyComponent: () -> Unit,
) {
    val glowPrimary = MaterialTheme.colorScheme.primary
    val glowSecondary = MaterialTheme.colorScheme.secondary
    val isShizukuConnected = SheveryBridge.isConnected(uiState.sheveryStatus)

    if (uiState.isInitializing) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowPrimary.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.12f),
                        radius = size.minDimension * 0.72f,
                    ),
                    radius = size.minDimension * 0.72f,
                    center = Offset(size.width * 0.5f, size.height * 0.12f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowSecondary.copy(alpha = 0.09f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.88f),
                        radius = size.minDimension * 0.6f,
                    ),
                    radius = size.minDimension * 0.6f,
                    center = Offset(size.width * 0.85f, size.height * 0.88f),
                )
            },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StatusBannerCard(
                    outcome = uiState.launchOutcome,
                    sheveryStatus = uiState.sheveryStatus,
                    sheveryDiagnostic = uiState.sheveryDiagnostic,
                    isGoogleTvInstalled = uiState.isGoogleTvInstalled,
                    isTargetInstalled = uiState.isTargetPackageInstalled,
                    onRequestSheveryPermission = onRequestSheveryPermission,
                    onOpenShevery = onOpenShevery,
                    onLaunchViaShevery = onLaunchViaShevery,
                    onInstallShizuku = onInstallShizuku,
                    onGetShevery = onGetShevery,
                    onOpenGoogleTv = onOpenGoogleTv,
                    onOpenPlayStore = onOpenPlayStore,
                    onInspectPackage = { pkg ->
                        val label = uiState.draftConfig.label.ifBlank { pkg }
                        onBrowseActivities(label, pkg)
                    },
                )

                SelectedTargetSummaryCard(
                    draftConfig = uiState.draftConfig,
                    isTargetInstalled = uiState.isTargetPackageInstalled,
                    sheveryStatus = uiState.sheveryStatus,
                    isLaunchingViaShevery = uiState.isLaunchingViaShevery,
                    onSheveryPillClick = {
                        when (uiState.sheveryStatus) {
                            BridgeStatus.PERMISSION_REQUIRED -> onRequestSheveryPermission()
                            BridgeStatus.NOT_INSTALLED -> Unit
                            else -> onOpenShevery()
                        }
                    },
                    onOpenPasteBox = onOpenPasteBox,
                    onCopyComponent = onCopyComponent,
                    onSaveAndLaunch = onSaveAndLaunch,
                    onSaveOnly = onSaveOnly,
                )

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val tabs = listOf(
                        SelectorTab.APPS to stringResource(R.string.tab_apps),
                        SelectorTab.QS_TILES to stringResource(R.string.tab_qs_tiles),
                        SelectorTab.PRESETS to stringResource(R.string.tab_presets),
                        SelectorTab.CUSTOM to stringResource(R.string.tab_custom),
                    )
                    tabs.forEachIndexed { index, (tab, title) ->
                        SegmentedButton(
                            selected = uiState.selectedTab == tab,
                            onClick = { onSelectTab(tab) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                            modifier = Modifier.testTag("tab_${tab.name.lowercase()}"),
                        ) {
                            Text(
                                text = title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                when (uiState.selectedTab) {
                    SelectorTab.APPS -> {
                        InstalledAppsTabContent(
                            apps = uiState.installedApps,
                            isLoading = uiState.isLoadingApps,
                            searchQuery = uiState.appSearchQuery,
                            selectedPackage = uiState.draftConfig.packageName,
                            selectedActivity = uiState.draftConfig.activityClass,
                            onSearchQueryChange = onSearchQueryChange,
                            onSelectApp = onSelectApp,
                            onBrowseActivities = { appInfo ->
                                onBrowseActivities(appInfo.label, appInfo.packageName)
                            },
                        )
                    }

                    SelectorTab.QS_TILES -> {
                        QsTilesTabContent(
                            tiles = uiState.qsTiles,
                            isLoading = uiState.isLoadingTiles,
                            searchQuery = uiState.tileSearchQuery,
                            selectedConfig = uiState.draftConfig,
                            isShizukuConnected = isShizukuConnected,
                            onSearchQueryChange = onTileSearchQueryChange,
                            onSelectTile = onSelectQsTile,
                            onLockedTileClick = { tile ->
                                onLockedTargetClick(
                                    tile.label,
                                    tile.packageName,
                                    ShizukuRequirementReason.QUICK_SETTINGS_TILE,
                                    null,
                                )
                            },
                        )
                    }

                    SelectorTab.PRESETS -> {
                        PresetsTabContent(
                            selectedConfig = uiState.draftConfig,
                            presetPreflight = uiState.presetPreflight,
                            isShizukuConnected = isShizukuConnected,
                            onSelectPreset = onSelectPreset,
                            onLockedPresetClick = { preset, preflight ->
                                onLockedTargetClick(
                                    preset.title,
                                    preset.config.packageName,
                                    preflight.reason,
                                    preflight.requiredPermission,
                                )
                            },
                        )
                    }

                    SelectorTab.CUSTOM -> {
                        CustomIntentTabContent(
                            draftConfig = uiState.draftConfig,
                            isTargetInstalled = uiState.isTargetPackageInstalled,
                            pasteText = uiState.pasteCommandText,
                            parsedResult = uiState.parsedAdbResult,
                            onPasteTextChange = onPasteTextChange,
                            onPasteFromClipboard = onPasteFromClipboard,
                            onApplyParsed = onApplyParsedCommand,
                            onRunParsed = onRunParsedCommand,
                            onUpdateLabel = onUpdateLabel,
                            onUpdatePackage = onUpdatePackage,
                            onUpdateActivity = onUpdateActivity,
                            onUpdateAction = onUpdateAction,
                            onUpdateDataUri = onUpdateDataUri,
                            onUpdateExtraArgs = onUpdateExtraArgs,
                            onUpdateKind = onUpdateKind,
                            onToggleAutoLaunch = onToggleAutoLaunch,
                            onTogglePrivilegedLaunch = onTogglePrivilegedLaunch,
                            onInspectActivities = {
                                val pkg = uiState.draftConfig.packageName
                                val label = uiState.draftConfig.label.ifBlank { pkg }
                                onBrowseActivities(label, pkg)
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    val inspection = uiState.inspectedAppResult
    if (inspection != null) {
        AppIntentsBottomSheet(
            inspection = inspection,
            defaultExportedOnly = !uiState.draftConfig.preferPrivilegedLaunch,
            isShizukuConnected = isShizukuConnected,
            onSelectIntentEntry = onSelectIntentEntry,
            onSelectActivity = onSelectActivity,
            onLockedIntentClick = { entry ->
                onLockedTargetClick(
                    entry.title,
                    entry.packageName,
                    entry.shizukuReason,
                    entry.requiredPermission,
                )
            },
            onLockedActivityClick = { activity ->
                onLockedTargetClick(
                    activity.shortName,
                    inspection.packageName,
                    activity.shizukuReason,
                    activity.requiredPermission,
                )
            },
            onDismiss = onDismissActivitiesSheet,
        )
    }

    val shizukuPrompt = uiState.shizukuHintPrompt
    if (shizukuPrompt != null) {
        ShizukuRequiredDialog(
            prompt = shizukuPrompt,
            diagnostic = uiState.sheveryDiagnostic,
            onInstallShizuku = {
                onDismissShizukuPrompt()
                onInstallShizuku()
            },
            onGetShevery = {
                onDismissShizukuPrompt()
                onGetShevery()
            },
            onOpenShevery = {
                onDismissShizukuPrompt()
                onOpenShevery()
            },
            onDismiss = onDismissShizukuPrompt,
        )
    }
}

@Composable
private fun ShizukuRequiredDialog(
    prompt: ShizukuHintPrompt,
    diagnostic: String,
    onInstallShizuku: () -> Unit,
    onGetShevery: () -> Unit,
    onOpenShevery: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isInstalledStopped = prompt.bridgeStatus != BridgeStatus.NOT_INSTALLED
    val targetName = prompt.targetLabel.ifBlank {
        prompt.packageName.ifBlank { "This intent" }
    }
    val reasonClause = when (prompt.reason) {
        ShizukuRequirementReason.QUICK_SETTINGS_TILE -> stringResource(R.string.reason_qs_tile)
        ShizukuRequirementReason.SHELL_EXTRA_FLAGS -> stringResource(R.string.reason_shell_extras)
        ShizukuRequirementReason.PERMISSION_REQUIRED -> stringResource(R.string.reason_permission_required)
        ShizukuRequirementReason.UNEXPORTED_ACTIVITY,
        ShizukuRequirementReason.NONE,
        -> stringResource(R.string.reason_unexported_activity)
    }

    val titleText = if (isInstalledStopped) {
        stringResource(R.string.status_shizuku_connect_title)
    } else {
        stringResource(R.string.status_shizuku_required_title)
    }

    val bodyText = when (prompt.bridgeStatus) {
        BridgeStatus.STUB_MISSING -> stringResource(
            R.string.status_shizuku_connect_message_stub,
            targetName,
        )
        BridgeStatus.SERVICE_NOT_RUNNING -> stringResource(
            R.string.status_shizuku_connect_message_stopped,
            targetName,
            reasonClause,
        )
        else -> when (prompt.reason) {
            ShizukuRequirementReason.QUICK_SETTINGS_TILE ->
                stringResource(R.string.status_shizuku_required_message_tile)
            ShizukuRequirementReason.SHELL_EXTRA_FLAGS ->
                stringResource(R.string.status_shizuku_required_message_extras)
            ShizukuRequirementReason.PERMISSION_REQUIRED -> stringResource(
                R.string.status_shizuku_required_message_permission,
                prompt.requiredPermission.orEmpty(),
            )
            ShizukuRequirementReason.UNEXPORTED_ACTIVITY,
            ShizukuRequirementReason.NONE,
            -> stringResource(R.string.status_shizuku_required_message_activity)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.AdminPanelSettings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isInstalledStopped && diagnostic.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = diagnostic,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
                }
                if (isInstalledStopped) {
                    TextButton(
                        onClick = onInstallShizuku,
                        modifier = Modifier.testTag("dialog_install_shizuku_secondary_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_install_shizuku))
                    }
                } else {
                    TextButton(
                        onClick = onGetShevery,
                        modifier = Modifier.testTag("dialog_get_shevery_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_get_shevery))
                    }
                }
            }
        },
        confirmButton = {
            if (isInstalledStopped) {
                Button(
                    onClick = onOpenShevery,
                    modifier = Modifier.testTag("dialog_open_shevery_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.AdminPanelSettings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_open_shevery))
                }
            } else {
                Button(
                    onClick = onInstallShizuku,
                    modifier = Modifier.testTag("dialog_install_shizuku_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_install_shizuku))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_dismiss_shizuku_button"),
            ) {
                Text(stringResource(R.string.action_dismiss))
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusBannerCard(
    outcome: LaunchOutcome,
    sheveryStatus: BridgeStatus,
    sheveryDiagnostic: String,
    isGoogleTvInstalled: Boolean,
    isTargetInstalled: Boolean,
    onRequestSheveryPermission: () -> Unit,
    onOpenShevery: () -> Unit,
    onLaunchViaShevery: () -> Unit,
    onInstallShizuku: () -> Unit,
    onGetShevery: () -> Unit,
    onOpenGoogleTv: () -> Unit,
    onOpenPlayStore: (String) -> Unit,
    onInspectPackage: (String) -> Unit,
) {
    val visual = when (outcome) {
        LaunchOutcome.Idle, LaunchOutcome.Launching, LaunchOutcome.Launched -> BannerVisual(
            icon = Icons.Filled.SettingsRemote,
            tint = MaterialTheme.colorScheme.primary,
            container = MaterialTheme.colorScheme.primaryContainer,
            title = stringResource(R.string.status_configure_title),
            message = stringResource(R.string.status_configure_message),
            missingPkg = null,
            inspectablePkg = null,
            showGoogleTvShortcut = false,
            showSheveryActions = sheveryStatus == BridgeStatus.PERMISSION_REQUIRED,
        )

        is LaunchOutcome.AppNotInstalled -> BannerVisual(
            icon = Icons.Filled.WarningAmber,
            tint = MaterialTheme.colorScheme.tertiary,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            title = stringResource(R.string.status_not_installed_title),
            message = stringResource(R.string.status_not_installed_message, outcome.packageName),
            missingPkg = outcome.packageName,
            inspectablePkg = null,
            showGoogleTvShortcut = false,
            showSheveryActions = false,
        )

        is LaunchOutcome.ActivityNotFound -> BannerVisual(
            icon = Icons.Filled.WarningAmber,
            tint = MaterialTheme.colorScheme.tertiary,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            title = stringResource(R.string.status_activity_not_found_title),
            message = stringResource(R.string.status_activity_not_found_message),
            missingPkg = null,
            inspectablePkg = outcome.packageName.takeIf { it.isNotBlank() && isTargetInstalled },
            showGoogleTvShortcut = outcome.packageName == TargetIntentConfig.DEFAULT_PACKAGE && isGoogleTvInstalled,
            showSheveryActions = false,
        )

        is LaunchOutcome.PermissionDenied -> BannerVisual(
            icon = Icons.Filled.Lock,
            tint = MaterialTheme.colorScheme.error,
            container = MaterialTheme.colorScheme.errorContainer,
            title = stringResource(R.string.status_permission_denied_title),
            message = stringResource(R.string.status_permission_denied_message),
            missingPkg = null,
            inspectablePkg = outcome.packageName.takeIf { it.isNotBlank() && isTargetInstalled },
            showGoogleTvShortcut = outcome.packageName == TargetIntentConfig.DEFAULT_PACKAGE && isGoogleTvInstalled,
            showSheveryActions = sheveryStatus != BridgeStatus.NOT_INSTALLED,
        )

        is LaunchOutcome.ShizukuInstallRequired -> {
            val messageRes = when {
                outcome.isQsTile -> R.string.status_shizuku_required_message_tile
                outcome.hasShellExtras -> R.string.status_shizuku_required_message_extras
                else -> R.string.status_shizuku_required_message_activity
            }
            BannerVisual(
                icon = Icons.Filled.Lock,
                tint = MaterialTheme.colorScheme.error,
                container = MaterialTheme.colorScheme.errorContainer,
                title = stringResource(R.string.status_shizuku_required_title),
                message = stringResource(messageRes),
                missingPkg = null,
                inspectablePkg = outcome.packageName.takeIf { it.isNotBlank() && isTargetInstalled },
                showGoogleTvShortcut = outcome.packageName == TargetIntentConfig.DEFAULT_PACKAGE && isGoogleTvInstalled,
                showSheveryActions = false,
                showInstallShizukuActions = true,
            )
        }

        LaunchOutcome.SheveryStubMissing -> BannerVisual(
            icon = Icons.Filled.AdminPanelSettings,
            tint = MaterialTheme.colorScheme.tertiary,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            title = stringResource(R.string.status_shevery_stub_missing_title),
            message = stringResource(R.string.status_shevery_stub_missing_message),
            missingPkg = null,
            inspectablePkg = null,
            showGoogleTvShortcut = false,
            showSheveryActions = true,
        )

        LaunchOutcome.SheveryServiceNotRunning -> BannerVisual(
            icon = Icons.Filled.AdminPanelSettings,
            tint = MaterialTheme.colorScheme.tertiary,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            title = stringResource(R.string.status_shevery_not_running_title),
            message = stringResource(R.string.status_shevery_not_running_message),
            missingPkg = null,
            inspectablePkg = null,
            showGoogleTvShortcut = false,
            showSheveryActions = true,
        )

        LaunchOutcome.SheveryPermissionRequired -> BannerVisual(
            icon = Icons.Filled.AdminPanelSettings,
            tint = MaterialTheme.colorScheme.primary,
            container = MaterialTheme.colorScheme.primaryContainer,
            title = stringResource(R.string.status_shevery_permission_title),
            message = stringResource(R.string.status_shevery_permission_message),
            missingPkg = null,
            inspectablePkg = null,
            showGoogleTvShortcut = false,
            showSheveryActions = true,
        )

        LaunchOutcome.InvalidTarget -> BannerVisual(
            icon = Icons.Filled.WarningAmber,
            tint = MaterialTheme.colorScheme.tertiary,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            title = stringResource(R.string.status_invalid_target_title),
            message = stringResource(R.string.status_invalid_target_message),
            missingPkg = null,
            inspectablePkg = null,
            showGoogleTvShortcut = false,
            showSheveryActions = false,
        )

        is LaunchOutcome.Failed -> BannerVisual(
            icon = Icons.Filled.WarningAmber,
            tint = MaterialTheme.colorScheme.error,
            container = MaterialTheme.colorScheme.errorContainer,
            title = stringResource(R.string.status_generic_error_title),
            message = outcome.detail?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.status_generic_error_message),
            missingPkg = null,
            inspectablePkg = null,
            showGoogleTvShortcut = false,
            showSheveryActions = sheveryStatus != BridgeStatus.NOT_INSTALLED,
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, visual.tint.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = visual.container,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = visual.icon,
                            contentDescription = stringResource(R.string.cd_remote_icon),
                            tint = visual.tint,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.launcher_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = visual.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Text(
                text = visual.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (visual.showSheveryActions && sheveryDiagnostic.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = sheveryDiagnostic,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }

            if (visual.missingPkg != null) {
                TextButton(
                    onClick = { onOpenPlayStore(visual.missingPkg) },
                    modifier = Modifier.testTag("install_missing_package_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shop,
                        contentDescription = stringResource(R.string.cd_external_app_icon),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_install_google_tv))
                }
            } else if (
                visual.showSheveryActions ||
                visual.showInstallShizukuActions ||
                visual.showGoogleTvShortcut ||
                visual.inspectablePkg != null
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (visual.showInstallShizukuActions) {
                        TextButton(
                            onClick = onInstallShizuku,
                            modifier = Modifier.testTag("banner_install_shizuku_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shop,
                                contentDescription = stringResource(R.string.cd_external_app_icon),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_install_shizuku))
                        }

                        TextButton(
                            onClick = onGetShevery,
                            modifier = Modifier.testTag("banner_get_shevery_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = stringResource(R.string.cd_external_app_icon),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_get_shevery))
                        }
                    }

                    if (visual.showSheveryActions) {
                        if (sheveryStatus == BridgeStatus.PERMISSION_REQUIRED) {
                            TextButton(
                                onClick = onRequestSheveryPermission,
                                modifier = Modifier.testTag("grant_shevery_permission_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.action_grant_shevery))
                            }
                        }

                        TextButton(
                            onClick = onOpenShevery,
                            modifier = Modifier.testTag("open_shevery_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = stringResource(R.string.cd_external_app_icon),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_open_shevery))
                        }

                        if (outcome != LaunchOutcome.Idle) {
                            TextButton(
                                onClick = onLaunchViaShevery,
                                modifier = Modifier.testTag("retry_shevery_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.action_retry_shevery))
                            }
                        }
                    }

                    if (visual.showGoogleTvShortcut) {
                        TextButton(
                            onClick = onOpenGoogleTv,
                            modifier = Modifier.testTag("open_google_tv_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = stringResource(R.string.cd_external_app_icon),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_open_google_tv))
                        }
                    }

                    if (visual.inspectablePkg != null) {
                        TextButton(
                            onClick = { onInspectPackage(visual.inspectablePkg) },
                            modifier = Modifier.testTag("banner_inspect_exported_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Layers,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_inspect_exported))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedTargetSummaryCard(
    draftConfig: TargetIntentConfig,
    isTargetInstalled: Boolean,
    sheveryStatus: BridgeStatus,
    isLaunchingViaShevery: Boolean,
    onSheveryPillClick: () -> Unit,
    onOpenPasteBox: () -> Unit,
    onCopyComponent: () -> Unit,
    onSaveAndLaunch: () -> Unit,
    onSaveOnly: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.label_active_target),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (draftConfig.preferPrivilegedLaunch && sheveryStatus != BridgeStatus.NOT_INSTALLED) {
                        val (sheveryTextRes, sheveryBg, sheveryFg) = when (sheveryStatus) {
                            BridgeStatus.READY -> Triple(
                                R.string.shevery_pill_ready,
                                EmeraldContainer,
                                EmeraldOk,
                            )

                            BridgeStatus.PERMISSION_REQUIRED -> Triple(
                                R.string.shevery_pill_permission,
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            )

                            BridgeStatus.STUB_MISSING -> Triple(
                                R.string.shevery_pill_stub,
                                MaterialTheme.colorScheme.tertiaryContainer,
                                MaterialTheme.colorScheme.onTertiaryContainer,
                            )

                            else -> Triple(
                                R.string.shevery_pill_stopped,
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(sheveryBg)
                                .clickable(onClick = onSheveryPillClick)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = stringResource(sheveryTextRes),
                                style = MaterialTheme.typography.labelSmall,
                                color = sheveryFg,
                            )
                        }
                    }

                    if (draftConfig.packageName.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (isTargetInstalled) {
                                        EmeraldContainer
                                    } else {
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    },
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    if (isTargetInstalled) {
                                        R.string.label_status_installed
                                    } else {
                                        R.string.label_status_missing
                                    },
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isTargetInstalled) {
                                    EmeraldOk
                                } else {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                },
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = draftConfig.label.ifBlank {
                            draftConfig.packageName.ifBlank { "No Target Selected" }
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = draftConfig.displayTarget,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                IconButton(
                    onClick = onOpenPasteBox,
                    modifier = Modifier.testTag("quick_paste_adb_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Terminal,
                        contentDescription = stringResource(R.string.action_paste_adb_tab),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                IconButton(
                    onClick = onCopyComponent,
                    modifier = Modifier.testTag("copy_component_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = stringResource(R.string.cd_copy_component),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onSaveAndLaunch,
                    enabled = draftConfig.isValid && !isLaunchingViaShevery,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("save_and_launch_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    if (isLaunchingViaShevery) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.cd_launch_icon),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_save_and_launch),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                OutlinedButton(
                    onClick = onSaveOnly,
                    enabled = draftConfig.isValid && !isLaunchingViaShevery,
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("save_only_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_save_only),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

private data class BannerVisual(
    val icon: ImageVector,
    val tint: Color,
    val container: Color,
    val title: String,
    val message: String,
    val missingPkg: String?,
    val inspectablePkg: String?,
    val showGoogleTvShortcut: Boolean,
    val showSheveryActions: Boolean,
    val showInstallShizukuActions: Boolean = false,
)
