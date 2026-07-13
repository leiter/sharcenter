package cut.the.crap.ui.content.posts

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.card_cd_collapse
import cut.the.crap.shared.resources.card_cd_expand
import cut.the.crap.shared.resources.card_cd_load_editor
import cut.the.crap.shared.resources.card_cd_toggle_favorite
import cut.the.crap.shared.resources.cd_copy_clipboard
import cut.the.crap.shared.resources.context_menu_delete
import cut.the.crap.shared.resources.context_menu_post_facebook
import cut.the.crap.shared.resources.context_menu_post_twitter
import cut.the.crap.shared.resources.context_menu_share
import cut.the.crap.shared.resources.facebook
import cut.the.crap.shared.resources.posts_cd_toggle_selection
import cut.the.crap.shared.resources.x
import cut.the.crap.data.domain.ContentItem
import cut.the.crap.tools.formatTimestampWithLocalizedFormatter
import cut.the.crap.ui.components.MenuItem
import cut.the.crap.ui.components.MyPopupMenu
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.components.api.ContentItemAction
import cut.the.crap.ui.components.api.TextAction
import cut.the.crap.ui.theme.PreviewAppThemeProvider
import cut.the.crap.ui.theme.PreviewThemeWrapper

@Composable
fun ContentItemCard(
    modifier: Modifier = Modifier,
    contentItem: ContentItem,
    action: (Action) -> Unit,
    isDragging: Boolean,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
) {
    val formattedDate = remember(contentItem.lastModified) {
        formatTimestampWithLocalizedFormatter(contentItem.lastModified)
    }

    val characterCount = calculateCharCount(contentItem.text)

    // State to track if content is expanded
    var isExpanded by remember { mutableStateOf(false) }

    // Estimate if content is likely more than 3 lines (rough estimate: ~40 chars per line)
    val isExpandable = contentItem.text.length > 120

    Card(
        modifier = modifier
            .combinedClickable(
                // Tap toggles selection while in batch mode, otherwise loads the item into the
                // editor. Long-press enters batch selection (replaces long-press drag-reorder).
                onClick = {
                    if (selectionMode) action(ContentItemAction.ToggleSelection(contentItem.id))
                    else action(ContentItemAction.Load(contentItem.id))
                },
                onLongClick = {
                    if (!selectionMode) action(ContentItemAction.EnterSelectionMode(contentItem.id))
                }
            )
            .then(
                if (selectionMode && isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.medium)
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                selectionMode && isSelected -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                isDragging -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                contentItem.isActive -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            },
            contentColor = when {
                selectionMode && isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                isDragging -> MaterialTheme.colorScheme.onSurfaceVariant
                contentItem.isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
        ) {
            // Row 1: Text display with expand button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = contentItem.text.ifEmpty { "(Empty)" },
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )

                // Expand/collapse button in top right
                if (isExpandable) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded }
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = stringResource(if (isExpanded) Res.string.card_cd_collapse else Res.string.card_cd_expand)
                        )
                    }
                }
            }

            // Row 2: Metadata (date, character count, category)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formattedDate,
                    modifier = Modifier.padding(4.dp)
                )

                Text(
                    text = "$characterCount chars",
                    modifier = Modifier.padding(4.dp)
                )

                contentItem.category?.let { category ->
                    Text(
                        text = category,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            // Row 3: Action buttons — or, in selection mode, a selection indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
              if (selectionMode) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = stringResource(Res.string.posts_cd_toggle_selection),
                    modifier = Modifier.padding(12.dp)
                )
              } else {
                Row {
                    IconButton(
                        onClick = {
                            action(ContentItemAction.ToggleFavorite(contentItem.id))
                        }
                    ) {
                        Icon(
                            imageVector = if (contentItem.isFavorite) Icons.Filled.Star else Icons.Filled.StarOutline,
                            contentDescription = stringResource(Res.string.card_cd_toggle_favorite)
                        )
                    }

                    IconButton(
                        onClick = {
                            action(ContentItemAction.CopyToClipboard(contentItem))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CopyAll,
                            contentDescription = stringResource(Res.string.cd_copy_clipboard)
                        )
                    }

                    IconButton(
                        onClick = {
                            action(ContentItemAction.Load(contentItem.id))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(Res.string.card_cd_load_editor)
                        )
                    }
                }

                // Overflow menu aligned to the right
                MyPopupMenu(
                    action = action,
                    menuItems = listOf(
                        MenuItem(Res.string.context_menu_post_twitter, iconRes = Res.drawable.x, actionPayload = ContentItemAction.PostOnTwitter(contentItem)),
                        MenuItem(Res.string.context_menu_post_facebook, iconRes = Res.drawable.facebook, actionPayload = ContentItemAction.PostOnFacebook(contentItem)),
                        MenuItem(Res.string.context_menu_share, Icons.Filled.Share, ContentItemAction.ShareViaSheet(contentItem)),
                        MenuItem(Res.string.context_menu_delete, Icons.Filled.Delete, ContentItemAction.Delete(contentItem.id))
                    )
                )
              }
            }
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
private fun Preview(
    @PreviewParameter(PreviewAppThemeProvider::class) theme: PreviewThemeWrapper,
) {
    theme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Preview for active item
            ContentItemCard(
                contentItem = ContentItem(
                    id = 1,
                    text = "This is an active content item with some sample text",
                    isActive = true,
                    isFavorite = false,
                    lastModified = System.currentTimeMillis()
                ),
                action = {},
                isDragging = false
            )

            // Preview for inactive item with favorite
            ContentItemCard(
                contentItem = ContentItem(
                    id = 2,
                    text = "This is a regular content item that is marked as favorite",
                    isActive = false,
                    isFavorite = true,
                    lastModified = System.currentTimeMillis() - 86400000
                ),
                action = {},
                isDragging = false
            )

            // Preview for dragging item
            ContentItemCard(
                contentItem = ContentItem(
                    id = 3,
                    text = "This item is being dragged. Notice the visual styling indicating drag state.",
                    isActive = false,
                    isFavorite = false,
                    lastModified = System.currentTimeMillis()
                ),
                action = {},
                isDragging = true
            )

            // Preview for long text item (expandable)
            ContentItemCard(
                contentItem = ContentItem(
                    id = 4,
                    text = "This is a very long content item with lots of text that will trigger the expand/collapse button. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                    isActive = false,
                    isFavorite = false,
                    lastModified = System.currentTimeMillis(),
                    category = "Important"
                ),
                action = {},
                isDragging = false
            )

            // Preview for empty item
            ContentItemCard(
                contentItem = ContentItem(
                    id = 5,
                    text = "",
                    isActive = false,
                    isFavorite = false,
                    lastModified = System.currentTimeMillis()
                ),
                action = {},
                isDragging = false
            )
        }
    }
}
