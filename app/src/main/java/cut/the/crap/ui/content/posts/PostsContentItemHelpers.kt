package cut.the.crap.ui.content.posts

import androidx.lifecycle.viewModelScope
import cut.the.crap.data.domain.ContentItem
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.tools.DescriptionParser
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
 * Bridges the Links list into the Posts editor: seeds a fresh post composed from [link]
 * (its URL plus the saved handles, hashtags, and keywords), persists it as the active item,
 * and shows it in the editor. Mirrors [loadContentItem] but for a brand-new item, so the
 * user lands on the Posts screen with the draft ready to edit and send.
 */
internal fun PostsViewModel.composePostFromLink(link: ContentLink) {
    val text = link.toComposedPostText()
    viewModelScope.launch {
        // Detach from any currently active item, then make the new draft the active one.
        contentItemRepository.clearActiveItem()
        val id = contentItemRepository.insert(ContentItem(text = text, isActive = true))
        activeItemId = id.toInt()
        internalScreenState.update {
            it.copy(
                focusedContentText = TextValueWrapper(
                    newText = text,
                    selection = Pair(text.length, text.length)
                ),
                filterExpanded = false
            )
        }
    }
}

/**
 * Builds the editor text for "Compose post from this link": the URL followed (when present)
 * by the link's saved handles (@), hashtags (#), and keywords, space-joined on a new line.
 */
fun ContentLink.toComposedPostText(): String {
    val parsed = DescriptionParser.parse(description)
    val markers = buildList {
        parsed.handles.forEach { add("@$it") }
        parsed.hashtags.forEach { add("#$it") }
        parsed.keywords.forEach { add(it) }
    }
    return if (markers.isEmpty()) link else "$link\n\n${markers.joinToString(" ")}"
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
