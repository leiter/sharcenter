package cut.the.crap.fake

import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.domain.ContentLinkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * Fake implementation of ContentLinkRepository for testing.
 * Uses in-memory storage with MutableStateFlow for reactive updates.
 */
class FakeContentLinkRepository : ContentLinkRepository {

    private val items = MutableStateFlow<List<ContentLink>>(emptyList())
    private val _focusedItem = MutableSharedFlow<ContentLink>()
    private var nextId = 1

    override val focusedItem: Flow<ContentLink> = _focusedItem

    override suspend fun insert(contentLink: ContentLink) {
        val newItem = if (contentLink.id == -1) {
            contentLink.copy(id = nextId++)
        } else {
            nextId = maxOf(nextId, contentLink.id + 1)
            contentLink
        }
        items.update { it + newItem }
    }

    override suspend fun update(contentLink: ContentLink) {
        items.update { list ->
            list.map { if (it.id == contentLink.id) contentLink else it }
        }
    }

    override suspend fun delete(contentLink: ContentLink) {
        items.update { list ->
            list.filter { it.id != contentLink.id }
        }
    }

    override suspend fun getContentLinkById(userId: Int): ContentLink? {
        return items.value.find { it.id == userId }
    }

    override fun getItems(
        includeFavourite: Boolean?,
        includeHidden: Boolean?,
        linkSubstring: String?,
        sortByListPosition: Boolean,
        sortByDate: Boolean,
        startTime: Long?,
        endTime: Long?
    ): Flow<List<ContentLink>> {
        return items.map { list ->
            var filtered = list

            // Filter by favourite
            if (includeFavourite != null) {
                filtered = filtered.filter { it.favourite == includeFavourite }
            }

            // Filter by hidden
            if (includeHidden != null) {
                filtered = filtered.filter { it.hideItem == includeHidden }
            }

            // Filter by link substring
            if (!linkSubstring.isNullOrBlank()) {
                filtered = filtered.filter {
                    it.link.contains(linkSubstring, ignoreCase = true) ||
                    it.description.contains(linkSubstring, ignoreCase = true)
                }
            }

            // Filter by time range
            if (startTime != null) {
                filtered = filtered.filter { it.added >= startTime }
            }
            if (endTime != null) {
                filtered = filtered.filter { it.added <= endTime }
            }

            // Sort
            when {
                sortByListPosition -> filtered.sortedBy { it.position }
                sortByDate -> filtered.sortedByDescending { it.added }
                else -> filtered
            }
        }
    }

    override suspend fun importFromFile(file: File) {
        // Not implemented for testing
    }

    override suspend fun byTimeRange(start: Long, end: Long): List<ContentLink> {
        return items.value.filter { it.added in start..end }
    }

    // Test helpers
    fun setItems(newItems: List<ContentLink>) {
        nextId = (newItems.maxOfOrNull { it.id } ?: 0) + 1
        items.value = newItems
    }

    fun getStoredItems(): List<ContentLink> = items.value

    fun reset() {
        items.value = emptyList()
        nextId = 1
    }

    suspend fun emitFocusedItem(item: ContentLink) {
        _focusedItem.emit(item)
    }
}
