package cut.the.crap.ui.content.links

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.links_cd_filtered
import cut.the.crap.shared.resources.links_cd_selected
import cut.the.crap.ui.components.MySearchBar
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.components.api.TextAction
import cut.the.crap.ui.components.api.UiAction

@Composable
fun ListStatusBar(
    textInputExpanded: Boolean,
    currentItemCount: Int,
    totalItemCount: Int,
    selectedItemCount: Int,
    selectionMode: Boolean = false,
    visibleItemIds: List<Int> = emptyList(),
    selectedItemIds: List<Int> = emptyList(),
    onSelectRemaining: (() -> Unit)? = null,
    searchQuery: String = "",
    searchExpanded: Boolean = false,
    action: ((Action) -> Unit)? = null
) {
    // Calculate how many visible items are NOT yet selected
    val remainingCount = if (selectionMode && visibleItemIds.isNotEmpty()) {
        visibleItemIds.count { it !in selectedItemIds }
    } else {
        currentItemCount - selectedItemCount
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = if (textInputExpanded) 0.dp else 16.dp,
                bottom = 8.dp
            ),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 16.dp)
        ) {
            if (selectionMode) {
                // Left side: 40% width for message/button - aligned to start
                Row(
                    modifier = Modifier.weight(0.4f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    // Icon always in same position
                    Icon(
                        imageVector = when {
                            visibleItemIds.isEmpty() -> Icons.Filled.SearchOff
                            else -> Icons.Filled.CheckCircle
                        },
                        contentDescription = when {
                            visibleItemIds.isEmpty() -> "No results"
                            remainingCount > 0 -> "Select remaining"
                            else -> "All selected"
                        },
                        tint = when {
                            visibleItemIds.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant
                            remainingCount > 0 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.tertiary
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Text changes based on state - both use same Text component for consistent positioning
                    Text(
                        text = when {
                            visibleItemIds.isEmpty() -> "No results"
                            remainingCount > 0 -> "+$remainingCount more"
                            else -> "All selected"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            visibleItemIds.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant
                            remainingCount > 0 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.tertiary
                        },
                        modifier = if (remainingCount > 0 && visibleItemIds.isNotEmpty()) {
                            Modifier.clickable { onSelectRemaining?.invoke() }
                        } else {
                            Modifier
                        }
                    )
                }

                // Right side: 60% width for search bar
                action?.let {
                    MySearchBar(
                        query = searchQuery,
                        expanded = searchExpanded,
                        onQueryChanged = { query -> it(TextAction.PostQuery(query)) },
                        onQuerySubmit = { },
                        modifier = Modifier.weight(0.6f, fill = true),
                        shape = MaterialTheme.shapes.medium,
                        onExpandedChanged = { expanded ->
                            it(UiAction.ExpandSearch(expanded, cut.the.crap.ui.components.api.Screen.Links))
                        }
                    )
                }
                Spacer(modifier = Modifier.width(if(!searchExpanded) 8.dp else 0.dp))
            } else {
                // Normal mode - show filter info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = stringResource(Res.string.links_cd_filtered),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentItemCount == totalItemCount) {
                            "Showing all $totalItemCount items"
                        } else {
                            "Showing $currentItemCount of $totalItemCount"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Right side - Selection indicator
                if (selectedItemCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(Res.string.links_cd_selected),
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$selectedItemCount selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}
