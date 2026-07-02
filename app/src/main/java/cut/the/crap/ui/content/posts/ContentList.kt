package cut.the.crap.ui.content.posts

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.max
import cut.the.crap.data.domain.ContentItem
//import cut.the.crap.mockedPostItems
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.theme.PreviewAppThemeProvider
import cut.the.crap.ui.theme.PreviewThemeWrapper
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ContentList(
    action: (Action) -> Unit,
    paddingValues: PaddingValues,
    contentItems: StateFlow<List<ContentItem>>,
    onContentItemsReordered: (List<ContentItem>) -> Unit,
) {

    val items by contentItems.collectAsState()

    val stateList = rememberLazyListState()

    var draggingItemIndex: Int? by remember {
        mutableStateOf(null)
    }

    var delta: Float by remember { mutableFloatStateOf(0f) }

    var draggingItem: LazyListItemInfo? by remember {
        mutableStateOf(null)
    }

    var currentItems by remember {
        mutableStateOf(items)
    }

    // Update currentItems only when not dragging
    LaunchedEffect(items) {
        if (draggingItemIndex == null) {
            // Not dragging - safe to update from database
            currentItems = items
        }
        // If dragging, ignore database updates until drag completes
    }

    val onMove = { fromIndex: Int, toIndex: Int ->
        // Bounds checking to prevent crash
        if (fromIndex in currentItems.indices && toIndex >= 0 && toIndex <= currentItems.size) {
            currentItems = currentItems.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
        }
    }

    val scrollChannel = Channel<Float>()

    LaunchedEffect(stateList) {
        while (true) {
            val diff = scrollChannel.receive()
            stateList.scrollBy(diff)
        }
    }

    // Get IME (keyboard) insets to add extra padding when keyboard is visible
    val imeInsets = WindowInsets.ime.asPaddingValues()
    val imeBottomPadding = imeInsets.calculateBottomPadding()

    LazyColumn(
        modifier = Modifier
            .pointerInput(key1 = stateList) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        // Convert touch offset to LazyColumn content coordinates
                        val adjustedOffset = offset.y + stateList.layoutInfo.viewportStartOffset

                        stateList.layoutInfo.visibleItemsInfo
                            .firstOrNull { item ->
                                adjustedOffset.toInt() in item.offset..(item.offset + item.size) &&
                                item.contentType is DraggableItem
                            }
                            ?.also {
                                val draggableItem = it.contentType as DraggableItem
                                draggingItem = it
                                draggingItemIndex = draggableItem.index
                            }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        delta += dragAmount.y

                        val currentDraggingItemIndex =
                            draggingItemIndex ?: return@detectDragGesturesAfterLongPress
                        val currentDraggingItem =
                            draggingItem ?: return@detectDragGesturesAfterLongPress

                        // Validate index is still valid
                        if (currentDraggingItemIndex !in currentItems.indices) {
                            draggingItemIndex = null
                            draggingItem = null
                            delta = 0f
                            return@detectDragGesturesAfterLongPress
                        }

                        val startOffset = currentDraggingItem.offset + delta
                        val endOffset =
                            currentDraggingItem.offset + currentDraggingItem.size + delta
                        val middleOffset = startOffset + (endOffset - startOffset) / 2

                        val targetItem =
                            stateList.layoutInfo.visibleItemsInfo.find { item ->
                                middleOffset.toInt() in item.offset..item.offset + item.size &&
                                    currentDraggingItem.index != item.index &&
                                    item.contentType is DraggableItem
                            }

                        if (targetItem != null) {
                            val targetIndex = (targetItem.contentType as DraggableItem).index
                            // Validate both indices before moving
                            if (currentDraggingItemIndex in currentItems.indices &&
                                targetIndex in currentItems.indices) {
                                onMove(currentDraggingItemIndex, targetIndex)
                                draggingItemIndex = targetIndex
                                delta += currentDraggingItem.offset - targetItem.offset
                                draggingItem = targetItem
                            }
                        } else {
                            val startOffsetToTop =
                                startOffset - stateList.layoutInfo.viewportStartOffset
                            val endOffsetToBottom =
                                endOffset - stateList.layoutInfo.viewportEndOffset
                            val scroll =
                                when {
                                    startOffsetToTop < 0 -> startOffsetToTop.coerceAtMost(0f)
                                    endOffsetToBottom > 0 -> endOffsetToBottom.coerceAtLeast(0f)
                                    else -> 0f
                                }
                            val canScrollDown =
                                currentDraggingItemIndex != currentItems.size - 1 && endOffsetToBottom > 0
                            val canScrollUp = currentDraggingItemIndex != 0 && startOffsetToTop < 0
                            if (scroll != 0f && (canScrollUp || canScrollDown)) {
                                scrollChannel.trySend(scroll)
                            }
                        }
                    },
                    onDragEnd = {
                        draggingItem = null
                        draggingItemIndex = null
                        delta = 0f
                        // Persist the new order to database
                        onContentItemsReordered(currentItems)
                    },
                    onDragCancel = {
                        draggingItem = null
                        draggingItemIndex = null
                        delta = 0f
                        // Reset to original order
                        currentItems = items
                    },
                )
            },
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
            items = currentItems,
            key = { _, item -> item.id },
            contentType = { index, _ -> DraggableItem(index = index) }) { index, item ->
            val modifier = if (draggingItemIndex == index) {
                Modifier
                    .zIndex(1f)
                    .graphicsLayer {
                        translationY = delta
                    }
            } else {
                Modifier
            }
            ContentItemCard(
                modifier = modifier,
                contentItem = item,
                isDragging = (draggingItemIndex != null && draggingItemIndex == index),
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
