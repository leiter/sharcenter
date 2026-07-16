package cut.the.crap.data.db
import cut.the.crap.tools.currentTimeMillis

import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.domain.ContentLinkRepository
import cut.the.crap.tools.DescriptionParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

enum class SortValue {
    SortedByListPosition,
    SortedByDate

    ;

    fun toBoolean():Boolean {
        return when(this.ordinal){
            SortedByDate.ordinal -> true
            SortedByListPosition.ordinal -> true
            else -> false
        }
    }
}
data class QueryConfig(
    val filterState: FilterState,
    val linkSubstring: String?,
    val sortState: SortState
)

data class FilterState(
    val includeFavourite: Boolean? = null,
    val includeHidden: Boolean? = null,
    val timeFrameStart: Long? = null, //currentTimeMillis() - 7 * 24 * 60 * 60 * 1000,
    val timeFrameEnd: Long? = null,  //currentTimeMillis(),
)

data class SortState(
    val reverse: Boolean = false,
    val sortValue: SortValue = SortValue.SortedByDate
)

class ItemManager(private val repository: ContentLinkRepository) {
    private val filterState = MutableStateFlow(FilterState())
    private val sortState = MutableStateFlow(SortState())

    val linkSubstring = MutableStateFlow("")

    private val queryConfig: Flow<QueryConfig> = combine(
        linkSubstring,
        filterState,
        sortState
    ) {
        linkSubstring,
        filterState,
        sortState ->
        QueryConfig(
            linkSubstring = linkSubstring,
            filterState = filterState,
            sortState = sortState,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredAndSortedItems: Flow<List<ContentLink>> =  queryConfig.flatMapLatest { queryConfig ->
        repository.getItems(
            includeFavourite = queryConfig.filterState.includeFavourite,
            includeHidden = queryConfig.filterState.includeHidden,
            // Search is applied in-memory below so it can match the item's keywords/
            // handles/hashtags (stored in `description`), not just the link URL.
            linkSubstring = null,
            sortByListPosition = queryConfig.sortState.sortValue.toBoolean(),
            sortByDate = queryConfig.sortState.sortValue.toBoolean(),
            startTime = queryConfig.filterState.timeFrameStart,
            endTime = queryConfig.filterState.timeFrameEnd

        ).map { items ->
            val query = queryConfig.linkSubstring
            val matched = if (query.isNullOrBlank()) {
                items
            } else {
                items.filter { it.matchesSearch(query) }
            }
            if (queryConfig.sortState.reverse) matched.reversed() else matched
        }
    }

    /**
     * Whether this link matches the search [query] (case-insensitive). It is an OR match:
     * the item matches if the query is contained in the link URL, the raw description text
     * (which includes YouTube metadata such as channel name and video title), or any of the
     * item's tags — handles, hashtags, or keywords — parsed from the serialized `description`.
     */
    private fun ContentLink.matchesSearch(query: String): Boolean {
        if (link.contains(query, ignoreCase = true)) return true
        if (description.contains(query, ignoreCase = true)) return true
        val parsed = DescriptionParser.parse(description)
        return (parsed.handles + parsed.hashtags + parsed.keywords)
            .any { it.contains(query, ignoreCase = true) }
    }

    fun triggerReload(){
        reverseOrder()
    }

    inline fun <T> T.toggleReverse(update: (T) -> T): T {
        return update(this)
    }

    fun filterByFavourite(include: Boolean?) {
        val newVal = filterState.value.copy(includeFavourite = include)
        filterState.value = newVal
    }

    fun updateFilterState(
        updates: Map<String, Any?>,
    ) {
        filterState.update { currentState ->
            currentState.copy(
                includeFavourite = updates.orDefault("includeFavourite", currentState.includeFavourite) as Boolean?,
                includeHidden = updates.orDefault("includeHidden", currentState.includeHidden) as Boolean?,
                timeFrameStart = updates.orDefault("timeFrameStart", currentState.timeFrameStart) as Long?,
                timeFrameEnd = updates.orDefault("timeFrameEnd", currentState.timeFrameEnd) as Long?
            )
        }
    }

    // `Map.getOrDefault` is a JVM-only extension; this preserves its exact semantics (return the
    // value only when the key is *present*, even if that value is null) for common code.
    private fun Map<String, Any?>.orDefault(key: String, default: Any?): Any? =
        if (containsKey(key)) this[key] else default

    fun filterByHidden(include: Boolean?) {
        val newVal = filterState.value.copy(includeHidden = include)
        filterState.value = newVal
    }

    fun filterByLinkSubstring(substring: String) {
        linkSubstring.value = substring
    }

    fun sortByListPosition() {
        val newVal = sortState.value.copy(sortValue = SortValue.SortedByListPosition)
        sortState.value = newVal
    }

    fun sortByDate() {
        val newVal = sortState.value.copy(sortValue = SortValue.SortedByDate)
        sortState.value = newVal
    }

    fun reverseOrder() {
        sortState.value = sortState.value.copy(reverse = !sortState.value.reverse)
    }
}
