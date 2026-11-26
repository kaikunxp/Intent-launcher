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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
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
import com.example.launcher.InstalledAppInfo
import com.example.launcher.IntentPreset
import com.example.launcher.PackageActivityInfo
import com.example.launcher.QsTileInfo
import com.example.launcher.RemoteActivityCandidate
import com.example.launcher.RemoteIntentLauncher.LaunchOutcome
import com.example.launcher.SheveryBridge.BridgeStatus
import com.example.launcher.TargetIntentConfig
import com.example.launcher.TargetKind
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOk

@Composable
fun RemoteLauncherScreen(
    uiState: RemoteLauncherUiState,
    snackbarHostState: SnackbarHostState,
    onSelectTab: (SelectorTab) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTileSearchQueryChange: (String) -> Unit,
    onSelectPreset: (IntentPreset) -> Unit,
    onSelectQsTile: (QsTileInfo) -> Unit,
    onSelectApp: (InstalledAppInfo) -> Unit,
    onBrowseActivities: (String, String) -> Unit,
    onSelectActivity: (PackageActivityInfo) -> Unit,
    onDismissActivitiesSheet: () -> Unit,
    onUpdateLabel: (String) -> Unit,
    onUpdatePackage: (String) -> Unit,
    onUpdateActivity: (String) -> Unit,
    onUpdateAction: (String) -> Unit,
    onUpdateDataUri: (String) -> Unit,
    onUpdateKind: (TargetKind) -> Unit,
    onToggleAutoLaunch: (Boolean) -> Unit,
    onTogglePrivilegedLaunch: (Boolean) -> Unit,
    onSaveAndLaunch: () -> Unit,
    onSaveAndLaunchConfig: (TargetIntentConfig) -> Unit,
    onSaveOnly: () -> Unit,
    onLaunchViaShevery: () -> Unit,
    onOpenShevery: () -> Unit,
    onOpenGoogleTv: () -> Unit,
    onOpenPlayStore: (String) -> Unit,
    onCopyComponent: () -> Unit,
) {
    val glowPrimary = MaterialTheme.colorScheme.primary
    val glowSecondary = MaterialTheme.colorScheme.secondary

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
        if (uiState.isInitializing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            return@Scaffold
        }

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
                    onOpenShevery = onOpenShevery,
                    onLaunchViaShevery = onLaunchViaShevery,
                    onOpenGoogleTv = onOpenGoogleTv,
                    onOpenPlayStore = onOpenPlayStore,
                    onInspectPackage = { pkg ->
                        val label = if (pkg == TargetIntentConfig.DEFAULT_PACKAGE) {
                            "Google TV"
                        } else {
                            uiState.draftConfig.label.ifBlank { pkg }
                        }
                        onBrowseActivities(label, pkg)
                    },
                )

                val remoteTiles = uiState.qsTiles.filter { it.isRemoteCandidate }
                val remoteActivities = uiState.discoveredRemoteActivities
                if (remoteTiles.isNotEmpty() || remoteActivities.isNotEmpty()) {
                    DiscoveredRemoteTargetsCard(
                        remoteTiles = remoteTiles.take(4),
                        remoteActivities = remoteActivities.take(5),
                        onLaunchTile = { tile ->
                            onSaveAndLaunchConfig(
                                TargetIntentConfig(
                                    label = "${tile.label} (QS Tile)",
                                    packageName = tile.packageName,
                                    activityClass = tile.serviceClass,
                                    autoLaunchEnabled = true,
                                    preferPrivilegedLaunch = true,
                                    kind = TargetKind.QS_TILE,
                                ),
                            )
                        },
                        onLaunchActivity = { candidate ->
                            onSaveAndLaunchConfig(
                                TargetIntentConfig(
                                    label = "${candidate.appLabel} (${candidate.shortName})",
                                    packageName = candidate.packageName,
                                    activityClass = candidate.className,
                                    autoLaunchEnabled = true,
                                    preferPrivilegedLaunch = true,
                                    kind = TargetKind.ACTIVITY,
                                ),
                            )
                        },
                    )
                }

                SelectedTargetSummaryCard(
                    draftConfig = uiState.draftConfig,
                    isTargetInstalled = uiState.isTargetPackageInstalled,
                    sheveryStatus = uiState.sheveryStatus,
                    isLaunchingViaShevery = uiState.isLaunchingViaShevery,
                    onCopyComponent = onCopyComponent,
                    onSaveAndLaunch = onSaveAndLaunch,
                    onSaveOnly = onSaveOnly,
                )

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val tabs = listOf(
                        SelectorTab.PRESETS to stringResource(R.string.tab_presets),
                        SelectorTab.QS_TILES to stringResource(R.string.tab_qs_tiles),
                        SelectorTab.APPS to stringResource(R.string.tab_apps),
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
                    SelectorTab.PRESETS -> {
                        PresetsTabContent(
                            selectedConfig = uiState.draftConfig,
                            onSelectPreset = onSelectPreset,
                        )
                    }

                    SelectorTab.QS_TILES -> {
                        QsTilesTabContent(
                            tiles = uiState.qsTiles,
                            isLoading = uiState.isLoadingTiles,
                            searchQuery = uiState.tileSearchQuery,
                            selectedConfig = uiState.draftConfig,
                            onSearchQueryChange = onTileSearchQueryChange,
                            onSelectTile = onSelectQsTile,
                        )
                    }

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

                    SelectorTab.CUSTOM -> {
                        CustomIntentTabContent(
                            draftConfig = uiState.draftConfig,
                            isTargetInstalled = uiState.isTargetPackageInstalled,
                            onUpdateLabel = onUpdateLabel,
                            onUpdatePackage = onUpdatePackage,
                            onUpdateActivity = onUpdateActivity,
                            onUpdateAction = onUpdateAction,
                            onUpdateDataUri = onUpdateDataUri,
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

    val inspectedApp = uiState.inspectedAppLabel
    if (inspectedApp != null) {
        ActivityInspectorBottomSheet(
            appLabel = inspectedApp,
            activities = uiState.inspectedActivities,
            defaultExportedOnly = !uiState.draftConfig.preferPrivilegedLaunch,
            onSelectActivity = onSelectActivity,
            onDismiss = onDismissActivitiesSheet,
        )
    }
}

@Composable
private fun DiscoveredRemoteTargetsCard(
    remoteTiles: List<QsTileInfo>,
    remoteActivities: List<RemoteActivityCandidate>,
    onLaunchTile: (QsTileInfo) -> Unit,
    onLaunchActivity: (RemoteActivityCandidate) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.label_discovered_on_device),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            remoteTiles.forEach { tile ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onLaunchTile(tile) }
                        .testTag("discovered_tile_${tile.serviceClass}"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${tile.label} • Quick Settings Tile",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${tile.packageName}/${tile.serviceClass.substringAfterLast('.')}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.cd_launch_icon),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            remoteActivities.forEach { candidate ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onLaunchActivity(candidate) }
                        .testTag("discovered_activity_${candidate.shortName}"),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SettingsRemote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${candidate.appLabel} • ${candidate.shortName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = candidate.className,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.cd_launch_icon),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusBannerCard(
    outcome: LaunchOutcome,
    sheveryStatus: BridgeStatus,
    sheveryDiagnostic: String,
    isGoogleTvInstalled: Boolean,
    isTargetInstalled: Boolean,
    onOpenShevery: () -> Unit,
    onLaunchViaShevery: () -> Unit,
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
            showSheveryActions = false,
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

        LaunchOutcome.SheveryStubMissing -> BannerVisual(
            icon = Icons.Filled.AdminPanelSettings,
            tint = MaterialTheme.colorScheme.tertiary,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            title = stringResource(R.string.status_shevery_stub_missing_title),
            message = stringResource(R.string.status_shevery_stub_missing_message),
            missingPkg = null,
            inspectablePkg = null,
            showGoogleTvShortcut = isGoogleTvInstalled,
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
            showGoogleTvShortcut = isGoogleTvInstalled,
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
            showGoogleTvShortcut = isGoogleTvInstalled,
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
            } else if (visual.showSheveryActions || visual.showGoogleTvShortcut || visual.inspectablePkg != null) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (visual.showSheveryActions) {
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
                        text = draftConfig.label.ifBlank { draftConfig.packageName.ifBlank { "Custom Intent" } },
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
)
