package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.launcher.AppInspectionResult
import com.example.launcher.AppIntentEntry
import com.example.launcher.PackageActivityInfo
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppIntentsBottomSheet(
    inspection: AppInspectionResult,
    defaultExportedOnly: Boolean,
    isShizukuConnected: Boolean,
    onSelectIntentEntry: (AppIntentEntry, Boolean) -> Unit,
    onSelectActivity: (PackageActivityInfo, Boolean) -> Unit,
    onLockedIntentClick: (AppIntentEntry) -> Unit,
    onLockedActivityClick: (PackageActivityInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        var exportedOnly by remember(inspection.packageName) { mutableStateOf(defaultExportedOnly) }
        var searchQuery by remember(inspection.packageName) { mutableStateOf("") }

        val query = searchQuery.trim().lowercase()
        val visibleIntents = inspection.intents.filter { entry ->
            val matchesExported = !exportedOnly || !entry.requiresShizuku
            val matchesQuery = query.isEmpty() ||
                entry.title.lowercase().contains(query) ||
                entry.subtitle.lowercase().contains(query) ||
                entry.badge.lowercase().contains(query) ||
                entry.action.lowercase().contains(query)
            matchesExported && matchesQuery
        }

        val visibleActivities = inspection.activities.filter { act ->
            val matchesExported = !exportedOnly || !act.requiresShizuku
            val matchesQuery = query.isEmpty() ||
                act.shortName.lowercase().contains(query) ||
                act.className.lowercase().contains(query) ||
                act.label.lowercase().contains(query)
            matchesExported && matchesQuery
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.sheet_activities_title, inspection.appLabel),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = inspection.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (inspection.intents.isNotEmpty() || inspection.activities.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sheet_search_intents_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text(stringResource(R.string.hint_search_activities)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.cd_search_icon),
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = stringResource(R.string.cd_clear_search),
                                )
                            }
                        }
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                FilterChip(
                    selected = exportedOnly,
                    onClick = { exportedOnly = !exportedOnly },
                    label = { Text(stringResource(R.string.filter_exported_only)) },
                    modifier = Modifier.testTag("filter_exported_only_chip"),
                )
                val hasLockedVisible = !isShizukuConnected && (
                    visibleIntents.any { it.requiresShizuku } ||
                        visibleActivities.any { it.requiresShizuku }
                    )
                if (hasLockedVisible) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.hint_shizuku_locked_items),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (inspection.intents.isEmpty() && inspection.activities.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_activities_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else if (visibleIntents.isEmpty() && visibleActivities.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_exported_activities_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (visibleIntents.isNotEmpty()) {
                        item(key = "header_intents") {
                            Text(
                                text = stringResource(R.string.label_discovered_intents_section),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                            )
                        }
                        items(
                            visibleIntents,
                            key = { "intent_${it.badge}_${it.componentClass}_${it.action}" },
                        ) { entry ->
                            AppIntentEntryRow(
                                entry = entry,
                                isShizukuConnected = isShizukuConnected,
                                onSelect = { onSelectIntentEntry(entry, false) },
                                onRunNow = { onSelectIntentEntry(entry, true) },
                                onLockedClick = { onLockedIntentClick(entry) },
                            )
                        }
                    }

                    if (visibleActivities.isNotEmpty()) {
                        item(key = "header_activities") {
                            Text(
                                text = stringResource(R.string.label_all_activities_section),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                            )
                        }
                        items(
                            visibleActivities,
                            key = { "act_${it.className}" },
                        ) { activity ->
                            DeclaredActivityRow(
                                activity = activity,
                                isShizukuConnected = isShizukuConnected,
                                onSelect = { onSelectActivity(activity, false) },
                                onRunNow = { onSelectActivity(activity, true) },
                                onLockedClick = { onLockedActivityClick(activity) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIntentEntryRow(
    entry: AppIntentEntry,
    isShizukuConnected: Boolean,
    onSelect: () -> Unit,
    onRunNow: () -> Unit,
    onLockedClick: () -> Unit,
) {
    val isLocked = entry.requiresShizuku && !isShizukuConnected
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                if (isLocked) onLockedClick() else onSelect()
            },
        shape = RoundedCornerShape(14.dp),
        color = if (isLocked) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isLocked) {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (isLocked) 0.52f else 1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(EmeraldContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = entry.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldOk,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (entry.requiresShizuku) {
                ShizukuBadgePill(isLocked = isLocked)
            }

            IconButton(onClick = { if (isLocked) onLockedClick() else onRunNow() }) {
                Icon(
                    imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (isLocked) R.string.cd_locked_shizuku else R.string.cd_launch_icon,
                    ),
                    tint = if (isLocked) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
    }
}

@Composable
private fun DeclaredActivityRow(
    activity: PackageActivityInfo,
    isShizukuConnected: Boolean,
    onSelect: () -> Unit,
    onRunNow: () -> Unit,
    onLockedClick: () -> Unit,
) {
    val isLocked = activity.requiresShizuku && !isShizukuConnected
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                if (isLocked) onLockedClick() else onSelect()
            },
        shape = RoundedCornerShape(14.dp),
        color = if (isLocked) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isLocked) {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (isLocked) 0.52f else 1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = activity.shortName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!activity.requiresShizuku) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EmeraldContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.label_exported),
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldOk,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = activity.className,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (activity.requiresShizuku) {
                ShizukuBadgePill(isLocked = isLocked)
            }

            IconButton(onClick = { if (isLocked) onLockedClick() else onRunNow() }) {
                Icon(
                    imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (isLocked) R.string.cd_locked_shizuku else R.string.cd_launch_icon,
                    ),
                    tint = if (isLocked) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
    }
}
