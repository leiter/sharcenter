package cut.the.crap.fake

import cut.the.crap.data.domain.ContentItem
import cut.the.crap.data.domain.ContentItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Fake implementation of ContentItemRepository for testing.
 * Uses in-memory storage with MutableStateFlow for reactive updates.
 */
class FakeContentItemRepository : ContentItemRepository {

    private val items = MutableStateFlow<List<ContentItem>>(emptyList())
    private var nextId = 1

    override suspend fun insert(contentItem: ContentItem): Long {
        val newItem = if (contentItem.id == 0) {
            contentItem.copy(id = nextId++)
        } else {
            nextId = maxOf(nextId, contentItem.id + 1)
            contentItem
        }
        items.update { it + newItem }
        return newItem.id.toLong()
    }

    override suspend fun update(contentItem: ContentItem) {
        items.update { list ->
            list.map { if (it.id == contentItem.id) contentItem else it }
        }
    }

    override suspend fun delete(contentItem: ContentItem) {
        items.update { list ->
            list.filter { it.id != contentItem.id }
        }
    }

    override suspend fun getById(id: Int): ContentItem? {
        return items.value.find { it.id == id }
    }

    override fun getItems(
        includeFavorite: Boolean?,
        sortByOrder: Boolean,
        sortByDate: Boolean,
        startTime: Long?,
        endTime: Long?
    ): Flow<List<ContentItem>> {
        return items.map { list ->
            var filtered = list

            // Filter by favorite
            if (includeFavorite != null) {
                filtered = filtered.filter { it.isFavorite == includeFavorite }
            }

            // Filter by time range
            if (startTime != null) {
                filtered = filtered.filter { it.created >= startTime }
            }
            if (endTime != null) {
                filtered = filtered.filter { it.created <= endTime }
            }

            // Sort
            when {
                sortByOrder -> filtered.sortedBy { it.sortOrder }
                sortByDate -> filtered.sortedByDescending { it.created }
                else -> filtered
            }
        }
    }

    override suspend fun getActiveItem(): ContentItem? {
        return items.value.find { it.isActive }
    }

    override suspend fun setActiveItem(id: Int) {
        items.update { list ->
            list.map {
                when {
                    it.id == id -> it.copy(isActive = true)
                    it.isActive -> it.copy(isActive = false)
                    else -> it
                }
            }
        }
    }

    override suspend fun clearActiveItem() {
        items.update { list ->
            list.map { if (it.isActive) it.copy(isActive = false) else it }
        }
    }

    override suspend fun toggleFavorite(id: Int, isFavorite: Boolean) {
        items.update { list ->
            list.map { if (it.id == id) it.copy(isFavorite = isFavorite) else it }
        }
    }

    override suspend fun updateSortOrder(id: Int, sortOrder: Int) {
        items.update { list ->
            list.map { if (it.id == id) it.copy(sortOrder = sortOrder) else it }
        }
    }

    override suspend fun updateSortOrders(items: List<ContentItem>) {
        items.forEachIndexed { index, item ->
            updateSortOrder(item.id, index)
        }
    }

    override suspend fun deleteAll() {
        items.value = emptyList()
    }

    // Test helpers
    fun setItems(newItems: List<ContentItem>) {
        nextId = (newItems.maxOfOrNull { it.id } ?: 0) + 1
        items.value = newItems
    }

    fun getStoredItems(): List<ContentItem> = items.value

    fun reset() {
        items.value = emptyList()
        nextId = 1
    }
}
