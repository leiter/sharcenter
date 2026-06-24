package cut.the.crap.data.db

import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.domain.ContentLinkRepository
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
    val timeFrameStart: Long? = null, //System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000,
    val timeFrameEnd: Long? = null,  //System.currentTimeMillis(),
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
            linkSubstring = queryConfig.linkSubstring,
            sortByListPosition = queryConfig.sortState.sortValue.toBoolean(),
            sortByDate = queryConfig.sortState.sortValue.toBoolean(),
            startTime = queryConfig.filterState.timeFrameStart,
            endTime = queryConfig.filterState.timeFrameEnd

        ).map { items -> if (queryConfig.sortState.reverse) items.reversed() else items }
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
                includeFavourite = updates.getOrDefault("includeFavourite", currentState.includeFavourite) as Boolean?,
                includeHidden = updates.getOrDefault("includeHidden", currentState.includeHidden) as Boolean?,
                timeFrameStart = updates.getOrDefault("timeFrameStart", currentState.timeFrameStart) as Long?,
                timeFrameEnd = updates.getOrDefault("timeFrameEnd", currentState.timeFrameEnd) as Long?
            )
        }
    }

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
