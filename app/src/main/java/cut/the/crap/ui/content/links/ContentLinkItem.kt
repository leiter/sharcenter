package cut.the.crap.ui.content.links

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import cut.the.crap.R
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.rest.YouTubeUrlParser
//import cut.the.crap.mockedLinkItems
import cut.the.crap.tools.DescriptionParser
import cut.the.crap.tools.LinkMetadata
import cut.the.crap.tools.domainPainter
import cut.the.crap.tools.formatTimestampWithLocalizedFormatter
import cut.the.crap.tools.getDisplayName
import cut.the.crap.tools.parseSocialMediaUrl
import cut.the.crap.tools.prepareUrlInformation
import cut.the.crap.ui.components.MenuItem
import cut.the.crap.ui.components.MyPopupMenu
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.components.api.ChipsType
import cut.the.crap.ui.components.api.ContentLinkAction
import cut.the.crap.ui.components.api.TextAction
import cut.the.crap.ui.theme.PreviewAppThemeProvider
import cut.the.crap.ui.theme.PreviewThemeWrapper
import kotlin.random.Random

@Composable
fun LinkListItem(
    item: ContentLink,
    action: (Action) -> Unit,
    isChecked: Boolean,
    selectionState: Boolean,
    modifier: Modifier = Modifier,
) {
    val texts = prepareUrlInformation(item.link)
    val formattedDate = remember(item.added) {
        formatTimestampWithLocalizedFormatter(item.added)
    }
    val socialInfo = remember(item.link) {
        parseSocialMediaUrl(item.link)
    }
    val isYouTube = remember(item.link) {
        YouTubeUrlParser.isYouTubeUrl(item.link)
    }

    // Parse structured description
    val parsed = remember(item.description) {
        DescriptionParser.parse(item.description)
    }
    val handles = parsed.handles
    val hashtags = parsed.hashtags
    val keywords = parsed.keywords

    // YouTube metadata from description
    val channelName = if (isYouTube) parsed.metadata.getOrNull(0)?.takeIf { it.isNotBlank() } else null
    val videoTitle = if (isYouTube) parsed.metadata.getOrNull(1)?.takeIf { it.isNotBlank() } else null
    val thumbnailUrl = if (isYouTube) parsed.metadata.getOrNull(2)?.takeIf { it.isNotBlank() } else null
    val contentType = if (isYouTube) parsed.metadata.getOrNull(3)?.takeIf { it.isNotBlank() } else null

    val allTagsEmpty = handles.isEmpty() && hashtags.isEmpty() && keywords.isEmpty()

    // State for quick actions visibility
    var showQuickActions by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .combinedClickable(
                onClick = {
                    if (selectionState) {
                        action(ContentLinkAction.ToggleSelection(item))
                    }
                },
                onLongClick = {
                    if (!selectionState) {
                        action(ContentLinkAction.EnterSelectionMode(item))
                    }
                }
            )
            .then(
                if (isChecked && selectionState) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.secondary,
                        shape = MaterialTheme.shapes.medium
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked && selectionState) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tags FlowRow
            if (allTagsEmpty) {
                Text(
                    text = "No keywords",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                )
            } else {
                FlowRow(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Handles (@) - secondary color
                    handles.forEach { handle ->
                        FilterChip(
                            selected = false,
                            onClick = if (!selectionState) {
                                {
                                    val updated = LinkMetadata.removeTag(item, handle, ChipsType.Handle)
                                    action(ContentLinkAction.EditSearchHint(item, updated.description))
                                }
                            } else {
                                { }
                            },
                            label = { Text("@$handle") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                            trailingIcon = if (!selectionState) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.links_cd_remove_handle),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null
                        )
                    }

                    // Hashtags (#) - tertiary color
                    hashtags.forEach { hashtag ->
                        FilterChip(
                            selected = false,
                            onClick = if (!selectionState) {
                                {
                                    val updated = LinkMetadata.removeTag(item, hashtag, ChipsType.Tag)
                                    action(ContentLinkAction.EditSearchHint(item, updated.description))
                                }
                            } else {
                                { }
                            },
                            label = { Text("#$hashtag") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            ),
                            trailingIcon = if (!selectionState) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.links_cd_remove_hashtag),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null
                        )
                    }

                    // Keywords - primary color
                    keywords.forEach { keyword ->
                        FilterChip(
                            selected = false,
                            onClick = if (!selectionState) {
                                {
                                    val updated = LinkMetadata.removeTag(item, keyword, ChipsType.KeyWords)
                                    action(ContentLinkAction.EditSearchHint(item, updated.description))
                                }
                            } else {
                                { }
                            },
                            label = { Text(keyword) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            trailingIcon = if (!selectionState) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.links_cd_remove_keyword),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            // Animated quick actions (only visible when not in selection mode)
            if (!selectionState) {
                AnimatedVisibility(
                    visible = showQuickActions,
                    enter = expandHorizontally(expandFrom = Alignment.End),
                    exit = shrinkHorizontally(shrinkTowards = Alignment.End)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Accounts button (@)
                        FilledTonalIconButton(
                            onClick = {
                                action(ContentLinkAction.ManageKeywords(item, ChipsType.Handle))
                            },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AlternateEmail,
                                contentDescription = stringResource(R.string.links_cd_add_accounts),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Hashtags button (#)
                        FilledTonalIconButton(
                            onClick = {
                                action(ContentLinkAction.ManageKeywords(item, ChipsType.Tag))
                            },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tag,
                                contentDescription = stringResource(R.string.links_cd_add_hashtags),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Keywords button (numbers)
                        FilledTonalIconButton(
                            onClick = {
                                action(ContentLinkAction.ManageKeywords(item, ChipsType.KeyWords))
                            },
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Numbers,
                                contentDescription = stringResource(R.string.links_cd_add_keywords),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Edit/Close toggle button
                IconButton(
                    onClick = { showQuickActions = !showQuickActions }
                ) {
                    Icon(
                        imageVector = if (showQuickActions) Icons.Filled.Close else Icons.Outlined.Edit,
                        contentDescription = if (showQuickActions) "Hide quick actions" else "Show quick actions"
                    )
                }
            }

            MyPopupMenu(
                action = action,
                menuItems = listOf(
                    MenuItem(R.string.context_menu_open, Icons.Filled.Link, ContentLinkAction.Open(item)),
                    MenuItem(R.string.context_menu_clipboard, Icons.Filled.CopyAll, ContentLinkAction.CopyToClipboard(item)),
                    MenuItem(R.string.context_menu_comment_quote, Icons.AutoMirrored.Filled.Comment, ContentLinkAction.ShowCommentQuoteDialog(item)),
                    MenuItem(R.string.context_menu_delete, Icons.Filled.Delete, ContentLinkAction.OfferDelete(item))
                )
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val hasDomain = texts[0].isNotBlank()
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .then(
                        if (hasDomain) {
                            Modifier.combinedClickable(
                                onClick = { },
                                onLongClick = {
                                    action(TextAction.AddHiddenFilter(texts[0]))
                                }
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = domainPainter(domain = texts[0]),
                    modifier = Modifier
                        .size(24.dp)
                        .then(
                            if (!hasDomain) {
                                Modifier.alpha(0.5f)
                            } else {
                                Modifier
                            }
                        ),
                    contentScale = ContentScale.Fit,
                    contentDescription = if (hasDomain) "Long press to filter by domain" else "No domain available"
                )
            }


            // For YouTube links, prefer channel name from metadata over URL-parsed username
            val displayUsername = if (isYouTube && channelName != null) {
                channelName
            } else {
                socialInfo?.username
            }
            val hasUsername = displayUsername != null && displayUsername.isNotBlank()
            Text(
                text = displayUsername ?: "No account",
                textAlign = TextAlign.Center,
                style = if (!hasUsername) {
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                modifier = Modifier
                    .weight(0.7f)
                    .then(
                        if (hasUsername) {
                            Modifier.combinedClickable(
                                onClick = { },
                                onLongClick = {
                                    // For YouTube with channel name, filter by channel name
                                    // For others, filter by URL path token
                                    val filterValue = if (isYouTube && channelName != null) {
                                        channelName
                                    } else {
                                        texts[1]
                                    }
                                    action(TextAction.AddHiddenFilter(filterValue))
                                }
                            )
                        } else {
                            Modifier
                        }
                    )
            )

            // Display content type badge - prefer metadata contentType for YouTube
            val displayType = if (isYouTube && contentType != null) {
                contentType.replaceFirstChar { it.uppercase() }
            } else {
                socialInfo?.getDisplayName()
            }

            if (displayType != null && displayType.isNotBlank()) {
                SuggestionChip(
                    onClick = { },
                    label = {
                        Text(
                            text = displayType,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            } else {
                SuggestionChip(
                    onClick = { },
                    label = {
                        Text(
                            text = "    ",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                )
            }

            Row {
                IconButton(
                    onClick = {
                        action(ContentLinkAction.ToggleFavourite(item))
                    }
                ) {
                    Icon(
                        imageVector = if (item.favourite) Icons.Filled.Star else Icons.Filled.StarOutline,
                        contentDescription = ""
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // YouTube thumbnail + title (left-aligned)
            if (thumbnailUrl != null) {
                val youTubePlaceholder = painterResource(id = R.drawable.youtube)
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = stringResource(R.string.links_cd_video_thumbnail),
                    placeholder = youTubePlaceholder,
                    error = youTubePlaceholder,
                    fallback = youTubePlaceholder,
                    modifier = Modifier
                        .size(48.dp, 36.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            if (videoTitle != null) {
                Text(
                    text = videoTitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (selectionState) {
                IconButton(
                    onClick = {
                        action(ContentLinkAction.ToggleSelection(item))
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        tint = if (isChecked) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        contentDescription = ""
                    )
                }
            }

            Text(
                text = formattedDate,
                modifier = Modifier
                    .padding(8.dp)
                    .combinedClickable(
                        onClick = { },
                        onLongClick = {
                            action(TextAction.SetStartDateFilter(
                                timestamp = item.added,
                                screen = cut.the.crap.ui.components.api.Screen.Links
                            ))
                        }
                    )
            )
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
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
//            (mockedLinkItems + mockedLinkItems).forEach {
//                LinkListItem(
//                    item = it,
//                    action = {},
//                    selectionState = Random.nextBoolean(),
//                    isChecked = Random.nextBoolean()
//                )
//            }

        }

    }
}
