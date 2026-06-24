package cut.the.crap.ui.content.posts

import androidx.lifecycle.viewModelScope
import cut.the.crap.data.domain.ContentItem
import cut.the.crap.tools.TextValueWrapper
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Extension functions for managing content items in PostsViewModel
 */

/**
 * Creates a new content item and clears the current editor state.
 * Ensures the filter is hidden and the content editor is shown.
 */
internal fun PostsViewModel.createNewContentItem() {
    viewModelScope.launch {
        // Clear current active item
        contentItemRepository.clearActiveItem()
        activeItemId = null
        // Reset text field, hide filter, and show content editor
        internalScreenState.update {
            it.copy(
                focusedContentText = TextValueWrapper(),
                filterExpanded = false
            )
        }
    }
}

/**
 * Loads a content item into the editor.
 * Ensures the filter is hidden and the content editor is shown.
 */
internal fun PostsViewModel.loadContentItem(item: ContentItem) {
    viewModelScope.launch {
        contentItemRepository.setActiveItem(item.id)
        activeItemId = item.id
        internalScreenState.update {
            it.copy(
                focusedContentText = TextValueWrapper(
                    newText = item.text,
                    selection = Pair(item.text.length, item.text.length)
                ),
                filterExpanded = false
            )
        }
    }
}

/**
 * Deletes a content item and handles loading the next item if needed
 */
internal fun PostsViewModel.deleteContentItem(item: ContentItem) {
    viewModelScope.launch {
        val wasActive = activeItemId == item.id

        // If deleting active item, clear editor state first
        if (wasActive) {
            contentItemRepository.clearActiveItem()
            activeItemId = null
            internalScreenState.update {
                it.copy(focusedContentText = TextValueWrapper())
            }
        }

        // Delete the item
        contentItemRepository.delete(item)

        // If it was the active item, load another one after deletion completes
        if (wasActive) {
            // Get fresh list after deletion
            val remainingItems = contentItemRepository.getItems().firstOrNull() ?: emptyList()
            if (remainingItems.isNotEmpty()) {
                loadContentItem(remainingItems.first())
            } else {
                // Already cleared above, just ensure state is clean
                createNewContentItem()
            }
        }
    }
}

/**
 * Toggles the favorite status of a content item
 */
internal fun PostsViewModel.toggleContentItemFavorite(item: ContentItem) {
    viewModelScope.launch {
        contentItemRepository.toggleFavorite(item.id, !item.isFavorite)
    }
}

/**
 * Updates the sort orders of multiple content items
 * Public function called from UI for drag-and-drop reordering
 */
fun PostsViewModel.updateContentItemSortOrders(items: List<ContentItem>) {
    viewModelScope.launch {
        contentItemRepository.updateSortOrders(items)
    }
}

/**
 * Sorts content items by their manual sort order
 */
internal fun PostsViewModel.sortContentByOrder() {
    contentItemManager.sortByOrder()
}

/**
 * Sorts content items by date (most recent first)
 */
internal fun PostsViewModel.sortContentByDate() {
    contentItemManager.sortByDate()
}

/**
 * Reverses the current content item order
 */
internal fun PostsViewModel.reverseContentOrder() {
    contentItemManager.reverseOrder()
}

/**
 * Auto-saves the content editor text to the database
 * Creates a new item if needed, or updates the existing active item
 */
internal fun PostsViewModel.autoSaveContentItem(textValue: TextValueWrapper) {
    viewModelScope.launch {
        if (activeItemId == null) {
            // Create new content item
            val newItem = ContentItem(
                text = textValue.newText,
                isActive = true
            )
            val id = contentItemRepository.insert(newItem)
            activeItemId = id.toInt()
        } else {
            // Update existing item
            val currentItem = contentItemRepository.getById(activeItemId!!)
            currentItem?.let {
                contentItemRepository.update(
                    it.copy(
                        text = textValue.newText,
                        lastModified = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
