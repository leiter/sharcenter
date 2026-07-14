package cut.the.crap.ui.content.links

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.links_active_filters
import cut.the.crap.shared.resources.links_bulk_delete
import cut.the.crap.shared.resources.links_bulk_deselect
import cut.the.crap.shared.resources.links_bulk_export
import cut.the.crap.shared.resources.links_bulk_favorite
import cut.the.crap.shared.resources.links_bulk_tag_accounts
import cut.the.crap.shared.resources.links_bulk_tag_hashtags
import cut.the.crap.shared.resources.links_bulk_tag_keywords
import cut.the.crap.shared.resources.links_cd_exit_selection
import cut.the.crap.shared.resources.links_cd_remove_filter
import cut.the.crap.shared.resources.links_clear_all
import cut.the.crap.shared.resources.links_deselect_all
import cut.the.crap.shared.resources.links_select_all_filtered
import cut.the.crap.shared.resources.menu_fire_job
import cut.the.crap.shared.resources.posts_cd_collapse_filters
import cut.the.crap.shared.resources.posts_cd_expand_filters
import cut.the.crap.ui.components.ActiveState
import cut.the.crap.ui.components.FilterState
import cut.the.crap.ui.components.MyChip
import cut.the.crap.ui.components.MyIconAction
import cut.the.crap.ui.components.MySearchBar
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.components.api.TextAction
import cut.the.crap.ui.components.api.UiAction
import cut.the.crap.ui.theme.PreviewAppThemeProvider
import cut.the.crap.ui.theme.PreviewThemeWrapper
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import cut.the.crap.ui.components.MenuItem
import cut.the.crap.ui.components.MyPopupMenu
import cut.the.crap.ui.components.api.ChipsType
import cut.the.crap.ui.components.api.FileAction
import cut.the.crap.ui.components.api.ListAction
import cut.the.crap.ui.components.api.Screen

@Composable
private fun FilterSection(
    screenState: LinksScreenState,
    currentItemCount: Int,
    totalItemCount: Int,
    showFilters: Boolean,
    selectionMode: Boolean = false,
    visibleItemIds: List<Int> = emptyList(),
    action: (Action) -> Unit
) {
    // Filter section
    AnimatedVisibility(showFilters) {
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                screenState.filterStateList.mapIndexed { index, item ->
                    MyChip(
                        onClick = { action(UiAction.ChipClicked(index, screen = cut.the.crap.ui.components.api.Screen.Links)) },
                        state = item,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        onLeadingClicked = if (item is FilterState.DateState && item.date != null) {
                            { action(UiAction.ClearDateFilter(item.dateType, cut.the.crap.ui.components.api.Screen.Links)) }
                        } else null
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // Show hidden filters as chips
    AnimatedVisibility(screenState.hiddenFilters.isNotEmpty()) {
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(Res.string.links_active_filters),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Show @ button if there are handle-like filters (not known domains)
                val knownDomains = setOf("x", "twitter", "youtube", "instagram", "facebook", "tiktok", "reddit", "linkedin", "threads", "mastodon", "bluesky", "tumblr", "pinterest", "snapchat", "twitch", "vimeo", "dailymotion")
                val hasHandleFilters = screenState.hiddenFilters.any { filter ->
                    !knownDomains.contains(filter.lowercase()) && filter.length > 1
                }
                if (hasHandleFilters) {
                    cut.the.crap.ui.components.MySymbolButton(
                        onClick = {
                            action(UiAction.ShowChips(cut.the.crap.ui.components.api.ChipsType.Handle, cut.the.crap.ui.components.api.Screen.Links))
                        },
                        symbol = "@",
                        isSelected = screenState.showHandleSelectionDialog
                    )
                }

                screenState.hiddenFilters.forEach { filter ->
                    AssistChip(
                        onClick = { action(TextAction.RemoveHiddenFilter(filter)) },
                        label = { Text(filter) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(Res.string.links_cd_remove_filter),
                                modifier = Modifier.padding(0.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }

                // Clear all button
                if (screenState.hiddenFilters.size > 1) {
                    AssistChip(
                        onClick = { action(TextAction.ClearHiddenFilters) },
                        label = { Text(stringResource(Res.string.links_clear_all)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Status bar showing filtered items count
    Box(modifier = Modifier.padding(horizontal = 8.dp)) {
        ListStatusBar(
            textInputExpanded = showFilters,
            currentItemCount = currentItemCount,
            totalItemCount = totalItemCount,
            selectedItemCount = screenState.selectedItems.size,
            selectionMode = selectionMode,
            visibleItemIds = visibleItemIds,
            selectedItemIds = screenState.selectedItems,
            onSelectRemaining = if (selectionMode) {
                { action(ListAction.SelectAll) }
            } else null,
            searchQuery = screenState.query,
            searchExpanded = screenState.searchExpanded,
            action = action
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SelectionModeTopBar(
    screenState: LinksScreenState,
    selectedCount: Int,
    currentItemCount: Int,
    totalItemCount: Int,
    visibleItemIds: List<Int> = emptyList(),
    onExitSelectionMode: () -> Unit,
    onSelectAllFiltered: () -> Unit,
    action: (Action) -> Unit
) {
    Column {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            navigationIcon = {
                IconButton(onClick = onExitSelectionMode) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(Res.string.links_cd_exit_selection)
                    )
                }
            },
            actions = {
                // Select All Filtered button - shows different icon/color based on action
                val allVisibleSelected = visibleItemIds.isNotEmpty() &&
                    visibleItemIds.all { it in screenState.selectedItems }

                IconButton(
                    onClick = onSelectAllFiltered,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = if (allVisibleSelected) {
                            Icons.Filled.Cancel
                        } else {
                            Icons.Filled.CheckCircle
                        },
                        contentDescription = if (allVisibleSelected) {
                            stringResource(Res.string.links_deselect_all)
                        } else {
                            stringResource(Res.string.links_select_all_filtered)
                        },
                        tint = if (allVisibleSelected) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Actions menu - only show when items are selected
                if (selectedCount > 0) {
                    MyPopupMenu(
                        action = action,
                        menuItems = listOf(
                            MenuItem(
                                title = Res.string.links_bulk_favorite,
                                icon = Icons.Filled.Favorite,
                                actionPayload = ListAction.ToggleFavoritesForSelected
                            ),
                            MenuItem(
                                title = Res.string.links_bulk_tag_accounts,
                                icon = Icons.Outlined.AlternateEmail,
                                actionPayload = UiAction.ShowBulkTagDialog(ChipsType.Handle, Screen.Links)
                            ),
                            MenuItem(
                                title = Res.string.links_bulk_tag_hashtags,
                                icon = Icons.Filled.Tag,
                                actionPayload = UiAction.ShowBulkTagDialog(ChipsType.Tag, Screen.Links)
                            ),
                            MenuItem(
                                title = Res.string.links_bulk_tag_keywords,
                                icon = Icons.Filled.Numbers,
                                actionPayload = UiAction.ShowBulkTagDialog(ChipsType.KeyWords, Screen.Links)
                            ),
                            MenuItem(
                                title = Res.string.links_bulk_export,
                                icon = Icons.Filled.Download,
                                actionPayload = FileAction.Export
                            ),
//                            MenuItem(
//                                title = Res.string.menu_fire_job,
//                                icon = Icons.Filled.Send,
//                                actionPayload = ListAction.FireJob
//                            ),
                            MenuItem(
                                title = Res.string.links_bulk_delete,
                                icon = Icons.Filled.Delete,
                                actionPayload = ListAction.DeleteSelected
                            ),
                            MenuItem(
                                title = Res.string.links_bulk_deselect,
                                icon = Icons.Filled.RemoveCircle,
                                actionPayload = ListAction.DeselectAll
                            )
                        )
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        )

        // Filter section - always shown in selection mode
        FilterSection(
            screenState = screenState,
            currentItemCount = currentItemCount,
            totalItemCount = totalItemCount,
            showFilters = true,
            selectionMode = true,
            visibleItemIds = visibleItemIds,
            action = action
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun linksTopBar(
    screenState: LinksScreenState,
    currentItemCount: Int,
    totalItemCount: Int,
    visibleItemIds: List<Int> = emptyList(),
    action: (Action) -> Unit,
): @Composable () -> Unit {
    return {
        if (screenState.checkMarks) {
            // Selection mode top bar
            SelectionModeTopBar(
                screenState = screenState,
                selectedCount = screenState.selectedItems.size,
                currentItemCount = currentItemCount,
                totalItemCount = totalItemCount,
                visibleItemIds = visibleItemIds,
                onExitSelectionMode = { action(UiAction.ExitSelectionMode(cut.the.crap.ui.components.api.Screen.Links)) },
                onSelectAllFiltered = {
                    // Toggle: if all visible are selected, deselect all; otherwise add all visible
                    val allVisibleSelected = visibleItemIds.isNotEmpty() &&
                        visibleItemIds.all { it in screenState.selectedItems }
                    if (allVisibleSelected) {
                        action(ListAction.DeselectAll)
                    } else {
                        action(ListAction.SelectAll)
                    }
                },
                action = action
            )
        } else {
            // Normal top bar
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("") },
                    actions = {
                    MySearchBar(
                        query = screenState.query,
                        expanded = screenState.searchExpanded,
                        onQueryChanged = { action(TextAction.PostQuery(it)) },
                        onQuerySubmit = { },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        onExpandedChanged = { action(UiAction.ExpandSearch(it, cut.the.crap.ui.components.api.Screen.Links)) }
                    )

                    // Determine if there are active filter chips
                    val hasActiveFilters = screenState.filterStateList.any { filter ->
                        when (filter) {
                            is FilterState.TripleState -> filter.activeState != ActiveState.Default
                            is FilterState.DateState -> filter.date != null
                            else -> false
                        }
                    }

                    MyIconAction(
                        iconPainter = rememberVectorPainter(
                            if (screenState.textInputExpanded)
                                Icons.Filled.KeyboardArrowUp
                            else
                                Icons.Filled.FilterList
                        ),
                        onClick = { action(UiAction.ExpandTextInput(!screenState.textInputExpanded, cut.the.crap.ui.components.api.Screen.Links)) },
                        contentDescription = stringResource(if (screenState.textInputExpanded) Res.string.posts_cd_collapse_filters else Res.string.posts_cd_expand_filters),
                        showBadge = hasActiveFilters && !screenState.textInputExpanded
                    )
                },
            )

                // Filter section
                FilterSection(
                    screenState = screenState,
                    currentItemCount = currentItemCount,
                    totalItemCount = totalItemCount,
                    showFilters = screenState.textInputExpanded,
                    visibleItemIds = visibleItemIds,
                    action = action
                )
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
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            
            linksTopBar(
                LinksScreenState(
                    query = "Search you might find",
                    searchExpanded = true,
                ),
                currentItemCount = 5,
                totalItemCount = 10,
            ) {}()

            linksTopBar(
                LinksScreenState(
                    query = "Search you might find",
                    searchExpanded = false,
                ),
                currentItemCount = 10,
                totalItemCount = 10,
            ) {}()

        }
    }
}
