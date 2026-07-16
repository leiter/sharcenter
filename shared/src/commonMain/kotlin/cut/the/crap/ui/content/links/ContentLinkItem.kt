package cut.the.crap.ui.content.links
import cut.the.crap.tools.urlSchemeAndHost

//import cut.the.crap.mockedLinkItems
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.bluesky
import cut.the.crap.shared.resources.context_menu_clipboard
import cut.the.crap.shared.resources.context_menu_comment_quote
import cut.the.crap.shared.resources.context_menu_compose_post
import cut.the.crap.shared.resources.context_menu_delete
import cut.the.crap.shared.resources.context_menu_open
import cut.the.crap.shared.resources.context_menu_show_channel
import cut.the.crap.shared.resources.context_menu_show_profile
import cut.the.crap.shared.resources.img_not_available
import cut.the.crap.shared.resources.links_cd_add_accounts
import cut.the.crap.shared.resources.links_cd_add_hashtags
import cut.the.crap.shared.resources.links_cd_add_keywords
import cut.the.crap.shared.resources.links_cd_remove_handle
import cut.the.crap.shared.resources.links_cd_remove_hashtag
import cut.the.crap.shared.resources.links_cd_remove_keyword
import cut.the.crap.shared.resources.links_cd_video_thumbnail
import cut.the.crap.shared.resources.mastodon
import cut.the.crap.shared.resources.reddit
import cut.the.crap.shared.resources.tiktok
import cut.the.crap.shared.resources.youtube
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.rest.YouTubeUrlParser
import cut.the.crap.tools.DescriptionParser
import cut.the.crap.tools.LinkMetadata
import cut.the.crap.tools.domainPainter
import cut.the.crap.tools.formatTimestampWithLocalizedFormatter
import cut.the.crap.tools.getDisplayName
import cut.the.crap.tools.isBlueskyUrl
import cut.the.crap.tools.isMastodonUrl
import cut.the.crap.tools.isRedditUrl
import cut.the.crap.tools.isTikTokUrl
import cut.the.crap.tools.parseSocialMediaUrl
import cut.the.crap.tools.prepareUrlInformation
import cut.the.crap.tools.profileUrl
import cut.the.crap.ui.components.MenuItem
import cut.the.crap.ui.components.MyPopupMenu
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.components.api.ChipsType
import cut.the.crap.ui.components.api.ContentLinkAction
import cut.the.crap.ui.components.api.TextAction

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
    val isBluesky = remember(item.link) {
        isBlueskyUrl(item.link)
    }
    val isMastodon = remember(item.link) {
        isMastodonUrl(item.link)
    }
    val isTikTok = remember(item.link) {
        isTikTokUrl(item.link)
    }
    val isReddit = remember(item.link) {
        isRedditUrl(item.link)
    }
    // Platforms that store rich metadata in the same positional layout in the description:
    // [primary name, secondary text, thumbnail, contentType] (YouTube channel/title, Bluesky,
    // Mastodon, TikTok & Reddit author/post text). Read and rendered through the shared block below.
    val hasRichMetadata = isYouTube || isBluesky || isMastodon || isTikTok || isReddit

    // Parse structured description
    val parsed = remember(item.description) {
        DescriptionParser.parse(item.description)
    }
    val handles = parsed.handles
    val hashtags = parsed.hashtags
    val keywords = parsed.keywords

    // Rich metadata from description (position 0-3), for YouTube and Bluesky links
    val channelName = if (hasRichMetadata) parsed.metadata.getOrNull(0)?.takeIf { it.isNotBlank() } else null
    val videoTitle = if (hasRichMetadata) parsed.metadata.getOrNull(1)?.takeIf { it.isNotBlank() } else null
    val thumbnailUrl = if (hasRichMetadata) parsed.metadata.getOrNull(2)?.takeIf { it.isNotBlank() } else null
    val contentType = if (hasRichMetadata) parsed.metadata.getOrNull(3)?.takeIf { it.isNotBlank() } else null

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
            // Keyword marker summary chips (or empty-state text)
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
                // Compact per-marker summary chips, inline with the edit pen. Each chip shows the
                // count for one marker type; tapping it opens a dropdown listing that type's
                // entries, each removable via the trailing delete icon.
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (handles.isNotEmpty()) {
                        MarkerSummaryChip(
                            icon = Icons.Outlined.AlternateEmail,
                            entries = handles,
                            entryPrefix = "@",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            enabled = !selectionState,
                            removeContentDescription = stringResource(Res.string.links_cd_remove_handle),
                            onRemove = { handle ->
                                val updated = LinkMetadata.removeTag(item, handle, ChipsType.Handle)
                                action(ContentLinkAction.EditSearchHint(item, updated.description))
                            }
                        )
                    }
                    if (hashtags.isNotEmpty()) {
                        MarkerSummaryChip(
                            icon = Icons.Filled.Tag,
                            entries = hashtags,
                            entryPrefix = "#",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            enabled = !selectionState,
                            removeContentDescription = stringResource(Res.string.links_cd_remove_hashtag),
                            onRemove = { hashtag ->
                                val updated = LinkMetadata.removeTag(item, hashtag, ChipsType.Tag)
                                action(ContentLinkAction.EditSearchHint(item, updated.description))
                            }
                        )
                    }
                    if (keywords.isNotEmpty()) {
                        MarkerSummaryChip(
                            icon = Icons.Filled.Numbers,
                            entries = keywords,
                            entryPrefix = "",
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            enabled = !selectionState,
                            removeContentDescription = stringResource(Res.string.links_cd_remove_keyword),
                            onRemove = { keyword ->
                                val updated = LinkMetadata.removeTag(item, keyword, ChipsType.KeyWords)
                                action(ContentLinkAction.EditSearchHint(item, updated.description))
                            }
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
                                contentDescription = stringResource(Res.string.links_cd_add_accounts),
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
                                contentDescription = stringResource(Res.string.links_cd_add_hashtags),
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
                                contentDescription = stringResource(Res.string.links_cd_add_keywords),
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

            // Channel / user profile URL derived from the link. Null when the link carries no
            // handle to resolve (e.g. a bare YouTube video), in which case no profile menu item
            // is offered at all.
            val profileUrl = socialInfo?.profileUrl()
            MyPopupMenu(
                action = action,
                menuItems = buildList {
                    add(MenuItem(Res.string.context_menu_open, Icons.Filled.Link, ContentLinkAction.Open(item)))
                    if (profileUrl != null) {
                        val isChannel = socialInfo.platform.equals("youtube", ignoreCase = true)
                        add(
                            MenuItem(
                                if (isChannel) Res.string.context_menu_show_channel else Res.string.context_menu_show_profile,
                                Icons.Outlined.AccountCircle,
                                ContentLinkAction.OpenProfile(item, profileUrl)
                            )
                        )
                    }
                    add(MenuItem(Res.string.context_menu_compose_post, Icons.AutoMirrored.Filled.Send, ContentLinkAction.ComposePost(item)))
                    add(MenuItem(Res.string.context_menu_clipboard, Icons.Filled.CopyAll, ContentLinkAction.CopyToClipboard(item)))
                    add(MenuItem(Res.string.context_menu_comment_quote, Icons.AutoMirrored.Filled.Comment, ContentLinkAction.ShowCommentQuoteDialog(item)))
                    add(MenuItem(Res.string.context_menu_delete, Icons.Filled.Delete, ContentLinkAction.OfferDelete(item)))
                }
            )
        }
        // When a thumbnail image is available, show it as a leading image that spans the
        // height of the two rows below (account/type row + title/date row), pushing their
        // content to the right. Falls back to the plain stacked rows when there is no image.
        val hasThumbnail = thumbnailUrl != null
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (hasThumbnail) Modifier.height(IntrinsicSize.Min) else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasThumbnail) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(16f / 9f)
                        .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    // Placeholder/fallback matches the platform (YouTube logo for YouTube,
                    // Bluesky logo for Bluesky, Mastodon logo for Mastodon, TikTok logo for TikTok,
                    // Reddit logo for Reddit, a neutral image otherwise) so a card never shows the
                    // wrong platform badge.
                    val thumbnailPlaceholder = painterResource(when {
                            isYouTube -> Res.drawable.youtube
                            isBluesky -> Res.drawable.bluesky
                            isMastodon -> Res.drawable.mastodon
                            isTikTok -> Res.drawable.tiktok
                            isReddit -> Res.drawable.reddit
                            else -> Res.drawable.img_not_available
                        }
                    )
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = stringResource(Res.string.links_cd_video_thumbnail),
                        placeholder = thumbnailPlaceholder,
                        error = thumbnailPlaceholder,
                        fallback = thumbnailPlaceholder,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    // Domain icon overlaid on the bottom-left corner of the thumbnail
                    DomainIcon(
                        domain = texts[0],
                        action = action,
                        modifier = Modifier.align(Alignment.BottomStart),
                        iconDomain = platformIconDomain(isMastodon, isReddit, texts[0]),
                        onClick = { action(ContentLinkAction.Open(item)) }
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Domain icon lives on the thumbnail (bottom-left) when there is one; otherwise show it here
            if (!hasThumbnail) {
                DomainIcon(
                    domain = texts[0],
                    action = action,
                    iconDomain = platformIconDomain(isMastodon, isReddit, texts[0]),
                    onClick = { action(ContentLinkAction.Open(item)) }
                )
            }


            // For YouTube/Bluesky, prefer the name from metadata (channel / author) over the
            // URL-parsed username
            val displayUsername = if (hasRichMetadata && channelName != null) {
                channelName
            } else {
                socialInfo?.username
            }
            val hasUsername = displayUsername != null && displayUsername.isNotBlank()
            // When no handle/channel is recognised, fall back to the domain (more useful than a
            // generic "No account"); keep the muted style so the card still reads as "unresolved".
            val domain = texts[0]
            val accountText = when {
                hasUsername -> displayUsername!!
                domain.isNotBlank() -> domain
                else -> "No account"
            }
            Text(
                text = accountText,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
                        // Interactive whenever there's something to act on — a recognised
                        // handle/channel, or the domain fallback. Only the bare "No account"
                        // state (no username, no domain) stays inert.
                        if (hasUsername || domain.isNotBlank()) {
                            Modifier.combinedClickable(
                                // Tap opens the account/channel profile on the platform when one is
                                // recognised; otherwise it visits the domain's home page. Falls back
                                // to the raw link only if neither can be resolved.
                                onClick = {
                                    val target = socialInfo?.profileUrl()
                                        ?: domainHomeUrl(item.link)
                                    if (target != null) {
                                        action(ContentLinkAction.OpenProfile(item, target))
                                    } else {
                                        action(ContentLinkAction.Open(item))
                                    }
                                },
                                onLongClick = {
                                    // Long-press filters by the value actually shown: the
                                    // YouTube/Bluesky metadata name, else the URL path token for a
                                    // recognised handle, else the domain fallback.
                                    val filterValue = when {
                                        hasRichMetadata && channelName != null -> channelName
                                        hasUsername -> texts[1]
                                        else -> domain
                                    }
                                    action(TextAction.AddHiddenFilter(filterValue))
                                }
                            )
                        } else {
                            Modifier
                        }
                    )
            )

            // Display content type badge - prefer metadata contentType for YouTube/Bluesky
            val displayType = if (hasRichMetadata && contentType != null) {
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
    }
}

/**
 * A compact summary chip for one marker type (handles / hashtags / keywords). Shows the entry
 * count with a leading category icon; tapping it opens a dropdown listing every entry, each with a
 * trailing delete icon that removes it via [onRemove]. Disabled (non-clickable) in selection mode.
 */
@Composable
private fun MarkerSummaryChip(
    icon: ImageVector,
    entries: List<String>,
    entryPrefix: String,
    containerColor: Color,
    labelColor: Color,
    enabled: Boolean,
    removeContentDescription: String,
    onRemove: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = false,
            enabled = enabled,
            onClick = { expanded = true },
            label = { Text(entries.size.toString()) },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = labelColor,
                    modifier = Modifier.size(18.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = containerColor,
                labelColor = labelColor,
            ),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text("$entryPrefix$entry") },
                    onClick = { onRemove(entry) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = removeContentDescription,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

/**
 * The domain-icon key for the badge. Most links use their derived domain, but some platforms don't
 * map cleanly to a single domain key — Mastodon is per-instance, and Reddit share/short links
 * (redd.it) reduce to a different key — so force the platform key for those. [fallback] is the
 * normally-derived domain (used for long-press domain filtering, which must stay the real domain).
 */
private fun platformIconDomain(isMastodon: Boolean, isReddit: Boolean, fallback: String): String =
    when {
        isMastodon -> "mastodon"
        isReddit -> "reddit"
        else -> fallback
    }

/**
 * The origin (scheme + host) of [link] — e.g. "https://www.youtube.com" — used to open the
 * platform's home page when the item carries no resolvable account/channel profile. The derived
 * `domain` text shown on the card is only a display token (TLD stripped), so the real host is
 * re-parsed from the link here. Returns null for a malformed link.
 */
private fun domainHomeUrl(link: String): String? =
    // Was java.net.URL (JVM-only, fully qualified). `scheme.lowercase()` reproduces
    // URL.getProtocol(), which lowercases; urlSchemeAndHost returns the scheme as parsed.
    urlSchemeAndHost(link)?.let { "${it.scheme.lowercase()}://${it.host}" }

@Composable
private fun DomainIcon(
    domain: String,
    action: (Action) -> Unit,
    modifier: Modifier = Modifier,
    // Which icon to show. Defaults to [domain]; callers override it for federated platforms
    // (e.g. Mastodon) where the per-instance domain has no single icon but the platform does.
    iconDomain: String = domain,
    // Tap action for the icon (opens the link). Long-press still filters by domain.
    onClick: () -> Unit = {},
) {
    val hasDomain = domain.isNotBlank()
    Box(
        modifier = modifier
            .size(48.dp)
            .then(
                if (hasDomain) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = { action(TextAction.AddHiddenFilter(domain)) }
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = domainPainter(domain = iconDomain),
            modifier = Modifier
                .size(24.dp)
                .then(if (!hasDomain) Modifier.alpha(0.5f) else Modifier),
            contentScale = ContentScale.Fit,
            contentDescription = if (hasDomain) "Tap to open link, long press to filter by domain" else "No domain available"
        )
    }
}
