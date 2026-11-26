package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.launcher.ParsedAdbResult
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOk

@Composable
fun CustomPasteBoxCard(
    pasteText: String,
    parsedResult: ParsedAdbResult?,
    onPasteTextChange: (String) -> Unit,
    onPasteFromClipboard: () -> Unit,
    onApplyParsed: () -> Unit,
    onRunParsed: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.label_paste_box_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                TextButton(
                    onClick = onPasteFromClipboard,
                    modifier = Modifier.testTag("paste_clipboard_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_paste_clipboard))
                }
            }

            OutlinedTextField(
                value = pasteText,
                onValueChange = onPasteTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("paste_adb_command_input"),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text(stringResource(R.string.hint_paste_box)) },
                trailingIcon = {
                    if (pasteText.isNotEmpty()) {
                        IconButton(onClick = { onPasteTextChange("") }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.cd_clear_search),
                            )
                        }
                    }
                },
            )

            if (parsedResult != null) {
                val isValid = parsedResult.config != null
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isValid) {
                        EmeraldContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (isValid) {
                            parsedResult.summary
                        } else {
                            parsedResult.error.orEmpty()
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isValid) {
                            EmeraldOk
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onApplyParsed,
                    enabled = parsedResult?.config != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("apply_parsed_command_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.action_apply_parsed))
                }

                Button(
                    onClick = onRunParsed,
                    enabled = parsedResult?.config != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("run_parsed_command_button"),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_apply_and_run))
                }
            }
        }
    }
}
