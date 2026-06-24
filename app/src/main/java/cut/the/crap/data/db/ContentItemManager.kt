package cut.the.crap.data.db

import cut.the.crap.data.domain.ContentItem
import cut.the.crap.data.domain.ContentItemRepository
import cut.the.crap.tools.toEndOfDay
import cut.the.crap.tools.toStartOfDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

data class ContentQueryConfig(
    val filterState: ContentFilterState,
    val sortState: ContentSortState
)

data class ContentFilterState(
    val includeFavorite: Boolean? = null,
    val timeFrameStart: Long? = null,
    val timeFrameEnd: Long? = null,
    val filterHandles: List<String> = emptyList(),  // List of handle texts to filter (e.g., "@username")
    val filterTags: List<String> = emptyList()      // List of tag texts to filter (e.g., "#hashtag")
)

data class ContentSortState(
    val reverse: Boolean = false,
    val sortByOrder: Boolean = true,
    val sortByDate: Boolean = false
)

class ContentItemManager(private val repository: ContentItemRepository) {
    private val filterState = MutableStateFlow(ContentFilterState())
    private val sortState = MutableStateFlow(ContentSortState())

    private val queryConfig: Flow<ContentQueryConfig> = combine(
        filterState,
        sortState
    ) { filterState, sortState ->
        ContentQueryConfig(
            filterState = filterState,
            sortState = sortState
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredAndSortedItems: Flow<List<ContentItem>> = queryConfig.flatMapLatest { config ->
        repository.getItems(
            includeFavorite = config.filterState.includeFavorite,
            sortByOrder = config.sortState.sortByOrder,
            sortByDate = config.sortState.sortByDate,
            startTime = config.filterState.timeFrameStart,
            endTime = config.filterState.timeFrameEnd
        ).map { items ->
            // Apply client-side text filtering for handles and tags
            val filteredItems = items.filter { item ->
                applyKeywordFilter(item, config.filterState)
            }
            if (config.sortState.reverse) filteredItems.reversed() else filteredItems
        }
    }

    /**
     * Applies keyword filtering (handles and tags) to a content item.
     * Returns true if the item should be included in results.
     *
     * Logic: Show item if:
     * - No keywords are selected (both lists empty), OR
     * - Item text contains ANY of the selected handles, OR
     * - Item text contains ANY of the selected tags
     */
    private fun applyKeywordFilter(item: ContentItem, filterState: ContentFilterState): Boolean {
        val hasHandleFilter = filterState.filterHandles.isNotEmpty()
        val hasTagFilter = filterState.filterTags.isNotEmpty()

        // If no filters are active, include all items
        if (!hasHandleFilter && !hasTagFilter) {
            return true
        }

        val itemText = item.text.lowercase()

        // Check if item contains any of the selected handles
        val matchesHandle = hasHandleFilter && filterState.filterHandles.any { handle ->
            itemText.contains(handle.lowercase())
        }

        // Check if item contains any of the selected tags
        val matchesTag = hasTagFilter && filterState.filterTags.any { tag ->
            itemText.contains(tag.lowercase())
        }

        // Include item if it matches any handle OR any tag
        return matchesHandle || matchesTag
    }

    fun filterByFavorite(include: Boolean?) {
        filterState.value = filterState.value.copy(includeFavorite = include)
    }

    fun filterByTimeFrame(start: Long?, end: Long?) {
        filterState.value = filterState.value.copy(
            timeFrameStart = start?.let { toStartOfDay(it) },
            timeFrameEnd = end?.let { toEndOfDay(it) }
        )
    }

    fun filterByKeywords(handles: List<String>, tags: List<String>) {
        filterState.value = filterState.value.copy(
            filterHandles = handles,
            filterTags = tags
        )
    }

    fun sortByOrder() {
        sortState.value = sortState.value.copy(sortByOrder = true, sortByDate = false)
    }

    fun sortByDate() {
        sortState.value = sortState.value.copy(sortByOrder = false, sortByDate = true)
    }

    fun reverseOrder() {
        sortState.value = sortState.value.copy(reverse = !sortState.value.reverse)
    }

}
