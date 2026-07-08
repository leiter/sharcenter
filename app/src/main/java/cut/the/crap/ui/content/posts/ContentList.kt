package cut.the.crap.ui.content.posts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.max
import cut.the.crap.data.domain.ContentItem
//import cut.the.crap.mockedPostItems
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.theme.PreviewAppThemeProvider
import cut.the.crap.ui.theme.PreviewThemeWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ContentList(
    action: (Action) -> Unit,
    paddingValues: PaddingValues,
    contentItems: StateFlow<List<ContentItem>>,
    // Reorder is temporarily disabled: long-press now drives batch selection instead of drag.
    // Kept in the signature so callers are undisturbed; reintroduce with a dedicated drag handle.
    onContentItemsReordered: (List<ContentItem>) -> Unit,
    selectionMode: Boolean = false,
    selectedItems: List<Int> = emptyList(),
) {

    val items by contentItems.collectAsState()

    val stateList = rememberLazyListState()

    // Get IME (keyboard) insets to add extra padding when keyboard is visible
    val imeInsets = WindowInsets.ime.asPaddingValues()
    val imeBottomPadding = imeInsets.calculateBottomPadding()

    LazyColumn(
        state = stateList,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = max(paddingValues.calculateBottomPadding(), imeBottomPadding + 16.dp - paddingValues.calculateBottomPadding())
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        itemsIndexed(
            items = items,
            key = { _, item -> item.id }
        ) { _, item ->
            ContentItemCard(
                contentItem = item,
                isDragging = false,
                selectionMode = selectionMode,
                isSelected = item.id in selectedItems,
                action = action
            )
        }

        // Spacer at the end to allow scrolling above FAB
        item {
            Spacer(
                modifier = Modifier.padding(bottom = 80.dp)
            )
        }
    }
}

// Content-type marker for drag-to-reorder. The Posts list no longer drags (long-press drives
// batch selection), but the Links list still references this type for its own reordering.
data class DraggableItem(val index: Int)

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
private fun Preview(
    @PreviewParameter(PreviewAppThemeProvider::class) theme: PreviewThemeWrapper,
) {
    theme {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        ) {
            // Preview with multiple content items showing drag capabilities
            ContentList(
                action = {},
                paddingValues = PaddingValues(0.dp),
                contentItems = MutableStateFlow(emptyList()),  // mockedPostItems.take(3)
                onContentItemsReordered = {},
            )
        }
    }
}
