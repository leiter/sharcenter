package cut.the.crap.ui.content.links

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import cut.the.crap.ui.components.ActiveState
import cut.the.crap.ui.components.DateType
import cut.the.crap.ui.components.FilterState

/**
 * Builds the filter chips list based on the current date filter state.
 * Creates appropriate combinations of favorite filters and date-related chips.
 *
 * @param startTime The start date timestamp (null if not set)
 * @param endTime The end date timestamp (null if not set)
 * @param favoriteActiveState Optional favorite filter active state to override current state
 * @return A list of FilterState objects representing the chips to display
 */
internal fun List<FilterState>.buildDateFilterChips(
    startTime: Long?,
    endTime: Long?,
    favoriteActiveState: ActiveState? = null
): List<FilterState> {
    val favoriteFilter = FilterState.TripleState(
        defaultLabel = "Favourites",
        iconPainterInclude = Icons.Filled.Favorite,
        iconPainterExclude = Icons.Outlined.FavoriteBorder,
        activeState = favoriteActiveState ?: this
            .filterIsInstance<FilterState.TripleState>()
            .firstOrNull()?.activeState ?: ActiveState.Default
    )

    val dateChips = when {
        // Both dates set: show two DateState chips
        startTime != null && endTime != null -> listOf(
            FilterState.DateState(
                defaultLabel = "Start Date",
                date = startTime,
                dateType = DateType.START
            ),
            FilterState.DateState(
                defaultLabel = "End Date",
                date = endTime,
                dateType = DateType.END
            )
        )
        // Only start date set: show start DateState + "Set end date" action
        startTime != null -> listOf(
            FilterState.DateState(
                defaultLabel = "Start Date",
                date = startTime,
                dateType = DateType.START
            ),
            FilterState.SingleActionState(
                defaultLabel = "Set end date",
                chosen = false
            )
        )
        // Only end date set: show "Set start date" action + end DateState
        endTime != null -> listOf(
            FilterState.SingleActionState(
                defaultLabel = "Set start date",
                chosen = false
            ),
            FilterState.DateState(
                defaultLabel = "End Date",
                date = endTime,
                dateType = DateType.END
            )
        )
        // No dates set: show single "Date Range" action
        else -> listOf(
            FilterState.SingleActionState(
                defaultLabel = "Date Range",
                chosen = false
            )
        )
    }

    return listOf(favoriteFilter) + dateChips
}

internal fun List<FilterState>.click(index: Int) : List<FilterState>{
    val item = when(val old = this[index]) {
        is FilterState.TripleState ->  old.copy(activeState = old.activeState.click())
        is FilterState.DateState -> old
        is FilterState.SingleActionState -> old.copy(chosen = !old.chosen)
    }
    val result = this.toMutableList()
    result[index] = item
    return result
}
