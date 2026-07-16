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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.cd_copy_clipboard
import cut.the.crap.shared.resources.chars
import cut.the.crap.shared.resources.context_menu_delete
import cut.the.crap.shared.resources.context_menu_post_facebook
import cut.the.crap.shared.resources.context_menu_post_twitter
import cut.the.crap.shared.resources.context_menu_share
import cut.the.crap.shared.resources.editor_cd_add_tag
import cut.the.crap.shared.resources.editor_cd_attach
import cut.the.crap.shared.resources.editor_cd_clear
import cut.the.crap.shared.resources.editor_cd_paste
import cut.the.crap.shared.resources.editor_cd_upload
import cut.the.crap.shared.resources.editor_placeholder
import cut.the.crap.shared.resources.editor_quick_actions
import cut.the.crap.shared.resources.editor_title
import cut.the.crap.shared.resources.facebook
import cut.the.crap.shared.resources.x
import cut.the.crap.tools.TextValueWrapper
import cut.the.crap.ui.components.MenuItem
import cut.the.crap.ui.components.MyPopupMenu
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.components.api.ChipsType
import cut.the.crap.ui.components.api.Screen
import cut.the.crap.ui.components.api.TextAction
import cut.the.crap.ui.components.api.UiAction
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            contentDescription = stringResource(Res.string.editor_cd_clear),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = stringResource(Res.string.editor_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pluralStringResource(Res.plurals.chars, charCount, charCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    // Overflow menu to the right of the char counter
                    MyPopupMenu(
                        action = onValueChange,
                        menuItems = listOf(
                            MenuItem(Res.string.context_menu_post_twitter, iconRes = Res.drawable.x, actionPayload = TextAction.PostContentOnTwitter),
                            MenuItem(Res.string.context_menu_post_facebook, iconRes = Res.drawable.facebook, actionPayload = TextAction.PostContentOnFacebook),
                            MenuItem(Res.string.context_menu_share, Icons.Filled.Share, TextAction.ShareContentViaSheet),
                            MenuItem(Res.string.context_menu_delete, Icons.Filled.Delete, TextAction.ClearContentText)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Text input field — underlines http(s) URLs and opens the tapped link.
            LinkFormattedTextField(
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
                placeholder = stringResource(Res.string.editor_placeholder),
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
                    text = stringResource(Res.string.editor_quick_actions),
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
                        contentDescription = stringResource(Res.string.editor_cd_add_tag),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Attach file button
//                FilledTonalIconButton(
//                    onClick = onPickFiles,
//                    modifier = Modifier.size(36.dp),
//                    colors = IconButtonDefaults.filledTonalIconButtonColors(
//                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
//                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                ) {
//                    Icon(
//                        imageVector = Icons.Filled.AttachFile,
//                        contentDescription = stringResource(Res.string.editor_cd_attach),
//                        modifier = Modifier.size(20.dp)
//                    )
//                }

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
                                contentDescription = stringResource(Res.string.editor_cd_upload),
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
                        contentDescription = stringResource(Res.string.editor_cd_paste),
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
                        contentDescription = stringResource(Res.string.cd_copy_clipboard),
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
