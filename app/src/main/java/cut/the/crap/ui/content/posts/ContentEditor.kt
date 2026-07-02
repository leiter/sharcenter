package cut.the.crap.ui.content.posts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPasteGo
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import cut.the.crap.tools.TextValueWrapper
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.components.api.ChipsType
import cut.the.crap.ui.components.api.Screen
import cut.the.crap.ui.components.api.TextAction
import cut.the.crap.ui.components.api.UiAction
import cut.the.crap.ui.theme.PreviewAppThemeProvider
import cut.the.crap.ui.theme.PreviewThemeWrapper
import kotlin.math.max
import kotlin.math.min

@Composable
fun ContentEditor(
    value: TextValueWrapper,
    onValueChange: (Action) -> Unit,
    modifier: Modifier = Modifier,
    selectedFileCount: Int = 0,
    isUploading: Boolean = false,
    onPickFiles: () -> Unit = {},
    onUploadFiles: () -> Unit = {}
) {
    val charCount = calculateCharCount(value.newText)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Compose",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$charCount chars",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    // Clear all text button
                    FilledTonalIconButton(
                        onClick = {
                            onValueChange(TextAction.ClearContentText)
                        },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Clear all text",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Text input field
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                value = value.toTextValue(),
                onValueChange = {
                    onValueChange(
                        TextAction.EditContentText(
                            value = TextValueWrapper(
                                it.text,
                                Pair(it.selection.min, it.selection.max)
                            )
                        )
                    )
                },
                placeholder = {
                    Text(
                        text = "What's on your mind?",
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                textStyle = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Quick actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick actions:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )

                // @ Handle button
                FilledTonalIconButton(
                    onClick = {
                        onValueChange(UiAction.ShowChips(ChipsType.Handle, Screen.Posts, source = "editor"))
                    },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) { Column {
                    Text(
                        text = "@",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                }

                // # Tag button
                FilledTonalIconButton(
                    onClick = {
                        onValueChange(UiAction.ShowChips(ChipsType.Tag, Screen.Posts, source = "editor"))
                    },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tag,
                        contentDescription = "Add tag",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Attach file button
                FilledTonalIconButton(
                    onClick = onPickFiles,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = "Attach files",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Upload button with badge (visible when files are selected)
                if (selectedFileCount > 0) {
                    Box {
                        FilledTonalIconButton(
                            onClick = onUploadFiles,
                            enabled = !isUploading,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = "Upload files",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Badge(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp),
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Text(
                                text = selectedFileCount.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Paste from clipboard button
                FilledTonalIconButton(
                    onClick = {
                        onValueChange(TextAction.PasteFromClipboard)
                    },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentPasteGo,
                        contentDescription = "Paste from clipboard",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Copy to clipboard button
                FilledTonalIconButton(
                    onClick = {
                        onValueChange(TextAction.CopyContentText)
                    },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy to clipboard",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun TextValueWrapper.toTextValue(): TextFieldValue {
    return TextFieldValue(
        text = newText,
        selection = selection.toTextRange()
    )
}

private fun Pair<Int, Int>.toTextRange(): TextRange {
    return if (this.first == this.second) TextRange(this.first)
    else TextRange(min(this.first, this.second), max(this.first, this.second))
}

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
private fun Preview(
    @PreviewParameter(PreviewAppThemeProvider::class) theme: PreviewThemeWrapper,
) {
    theme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preview with empty content
            ContentEditor(
                value = TextValueWrapper("", Pair(0, 0)),
                onValueChange = {}
            )

            // Preview with some text
            ContentEditor(
                value = TextValueWrapper("This is a sample content being edited", Pair(0, 0)),
                onValueChange = {}
            )

            // Preview with long text
            ContentEditor(
                value = TextValueWrapper(
                    "This is a longer content item that spans multiple lines. It demonstrates how the editor handles text wrapping and displays character count for longer content.",
                    Pair(0, 0)
                ),
                onValueChange = {}
            )
        }
    }
}
