package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.launcher.InstalledAppInfo
import com.example.launcher.IntentPreset
import com.example.launcher.ParsedAdbResult
import com.example.launcher.QsTileInfo
import com.example.launcher.TargetIntentConfig
import com.example.launcher.TargetKind

@Composable
fun PresetsTabContent(
    selectedConfig: TargetIntentConfig,
    onSelectPreset: (IntentPreset) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TargetIntentConfig.Presets.forEach { preset ->
            val isSelected = selectedConfig.kind == preset.config.kind &&
                selectedConfig.packageName == preset.config.packageName &&
                selectedConfig.activityClass == preset.config.activityClass &&
                selectedConfig.action == preset.config.action

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectPreset(preset) }
                    .testTag("preset_${preset.id}"),
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    val icon = when (preset.id) {
                        "google_tv_remote_legacy" -> Icons.Filled.SettingsRemote
                        "google_tv_app" -> Icons.Filled.Tv
                        else -> Icons.Filled.Apps
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = preset.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QsTilesTabContent(
    tiles: List<QsTileInfo>,
    isLoading: Boolean,
    searchQuery: String,
    selectedConfig: TargetIntentConfig,
    onSearchQueryChange: (String) -> Unit,
    onSelectTile: (QsTileInfo) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_tiles_input"),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text(stringResource(R.string.hint_search_tiles)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.cd_search_icon),
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.cd_clear_search),
                        )
                    }
                }
            },
        )

        val query = searchQuery.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            tiles
        } else {
            tiles.filter {
                it.label.lowercase().contains(query) ||
                    it.appLabel.lowercase().contains(query) ||
                    it.packageName.lowercase().contains(query) ||
                    it.serviceClass.lowercase().contains(query)
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            filtered.isEmpty() -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.empty_tiles_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.empty_tiles_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filtered.forEach { tile ->
                        val isSelected = selectedConfig.kind == TargetKind.QS_TILE &&
                            selectedConfig.packageName == tile.packageName &&
                            selectedConfig.activityClass == tile.serviceClass
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectTile(tile) }
                                .testTag("qs_tile_${tile.serviceClass}"),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (tile.icon != null) {
                                    Image(
                                        bitmap = tile.icon,
                                        contentDescription = tile.label,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                    )
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(38.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Tune,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tile.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${tile.appLabel} • ${tile.serviceClass.substringAfterLast('.')}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InstalledAppsTabContent(
    apps: List<InstalledAppInfo>,
    isLoading: Boolean,
    searchQuery: String,
    selectedPackage: String,
    selectedActivity: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectApp: (InstalledAppInfo) -> Unit,
    onBrowseActivities: (InstalledAppInfo) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_apps_input"),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text(stringResource(R.string.hint_search_apps)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.cd_search_icon),
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.cd_clear_search),
                        )
                    }
                }
            },
        )

        val query = searchQuery.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.label.lowercase().contains(query) ||
                    it.packageName.lowercase().contains(query) ||
                    it.launcherActivity.lowercase().contains(query)
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            filtered.isEmpty() -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.empty_apps_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.empty_apps_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filtered.forEach { appInfo ->
                        val isSelected = selectedPackage == appInfo.packageName &&
                            selectedActivity == appInfo.launcherActivity
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onBrowseActivities(appInfo) }
                                .testTag("app_item_${appInfo.packageName}"),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (appInfo.icon != null) {
                                    Image(
                                        bitmap = appInfo.icon,
                                        contentDescription = stringResource(
                                            R.string.cd_app_icon,
                                            appInfo.label,
                                        ),
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                    )
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Apps,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = appInfo.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = appInfo.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }

                                if (appInfo.hasLauncherEntry) {
                                    TextButton(
                                        onClick = { onSelectApp(appInfo) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.action_use_main_launcher),
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = { onBrowseActivities(appInfo) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Layers,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.action_browse_activities),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomIntentTabContent(
    draftConfig: TargetIntentConfig,
    isTargetInstalled: Boolean,
    pasteText: String,
    parsedResult: ParsedAdbResult?,
    onPasteTextChange: (String) -> Unit,
    onPasteFromClipboard: () -> Unit,
    onApplyParsed: () -> Unit,
    onRunParsed: () -> Unit,
    onUpdateLabel: (String) -> Unit,
    onUpdatePackage: (String) -> Unit,
    onUpdateActivity: (String) -> Unit,
    onUpdateAction: (String) -> Unit,
    onUpdateDataUri: (String) -> Unit,
    onUpdateExtraArgs: (String) -> Unit,
    onUpdateKind: (TargetKind) -> Unit,
    onToggleAutoLaunch: (Boolean) -> Unit,
    onTogglePrivilegedLaunch: (Boolean) -> Unit,
    onInspectActivities: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CustomPasteBoxCard(
            pasteText = pasteText,
            parsedResult = parsedResult,
            onPasteTextChange = onPasteTextChange,
            onPasteFromClipboard = onPasteFromClipboard,
            onApplyParsed = onApplyParsed,
            onRunParsed = onRunParsed,
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val kinds = listOf(
                TargetKind.ACTIVITY to "Activity Intent",
                TargetKind.QS_TILE to "QS Tile Click",
            )
            kinds.forEachIndexed { index, (kind, title) ->
                SegmentedButton(
                    selected = draftConfig.kind == kind,
                    onClick = { onUpdateKind(kind) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = kinds.size),
                ) {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        OutlinedTextField(
            value = draftConfig.label,
            onValueChange = onUpdateLabel,
            label = { Text(stringResource(R.string.label_target_label)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_label_input"),
        )

        OutlinedTextField(
            value = draftConfig.packageName,
            onValueChange = onUpdatePackage,
            label = { Text(stringResource(R.string.label_package)) },
            placeholder = { Text(stringResource(R.string.hint_package)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_package_input"),
        )

        OutlinedTextField(
            value = draftConfig.activityClass,
            onValueChange = onUpdateActivity,
            label = {
                Text(
                    if (draftConfig.kind == TargetKind.QS_TILE) {
                        "TileService Class"
                    } else {
                        stringResource(R.string.label_activity)
                    },
                )
            },
            placeholder = { Text(stringResource(R.string.hint_activity)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_activity_input"),
        )

        if (draftConfig.kind == TargetKind.ACTIVITY && isTargetInstalled && draftConfig.packageName.isNotBlank()) {
            OutlinedButton(
                onClick = onInspectActivities,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("inspect_package_activities_button"),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Layers,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_inspect_exported))
            }
        }

        if (draftConfig.kind == TargetKind.ACTIVITY) {
            OutlinedTextField(
                value = draftConfig.action,
                onValueChange = onUpdateAction,
                label = { Text(stringResource(R.string.label_action)) },
                placeholder = { Text(stringResource(R.string.hint_action)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_action_input"),
            )

            OutlinedTextField(
                value = draftConfig.dataUri,
                onValueChange = onUpdateDataUri,
                label = { Text(stringResource(R.string.label_data_uri)) },
                placeholder = { Text(stringResource(R.string.hint_data_uri)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_uri_input"),
            )

            OutlinedTextField(
                value = draftConfig.extraArgs,
                onValueChange = onUpdateExtraArgs,
                label = { Text(stringResource(R.string.label_extra_args)) },
                placeholder = { Text(stringResource(R.string.hint_extra_args)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_extra_args_input"),
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.label_auto_launch),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.label_auto_launch_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = draftConfig.autoLaunchEnabled,
                    onCheckedChange = onToggleAutoLaunch,
                    modifier = Modifier.testTag("auto_launch_switch"),
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.label_use_shevery),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.label_use_shevery_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = draftConfig.preferPrivilegedLaunch,
                    onCheckedChange = onTogglePrivilegedLaunch,
                    modifier = Modifier.testTag("use_shevery_switch"),
                )
            }
        }
    }
}
