package cut.the.crap.ui.content.posts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import cut.the.crap.data.domain.KeyWord
import cut.the.crap.ui.components.ActiveState
import cut.the.crap.ui.components.DateType
import cut.the.crap.ui.components.FilterState

/**
 * Builds the filter chips list for the Posts screen based on current filter state.
 * Creates appropriate combinations of handle, tag, favorite, and date-related chips.
 *
 * @param startTime The start date timestamp (null if not set)
 * @param endTime The end date timestamp (null if not set)
 * @param selectedHandleChips List of selected handles to show as chips
 * @param selectedTagChips List of selected tags to show as chips
 * @param favoriteActiveState Optional favorite filter active state to override current state
 * @return A list of FilterState objects representing the chips to display
 */
internal fun List<FilterState>.buildDateFilterChips(
    startTime: Long?,
    endTime: Long?,
    selectedHandleChips: List<KeyWord> = emptyList(),
    selectedTagChips: List<KeyWord> = emptyList(),
    favoriteActiveState: ActiveState? = null
): List<FilterState> {
    // @ and # chips for handle and tag filtering (FIRST - at the beginning)
    // These chips don't reflect state, they only open dialogs
    val handleChip = FilterState.SingleActionState(
        defaultLabel = "  @  ",  // Two leading and trailing spaces
        chosen = false
    )

    val tagChip = FilterState.SingleActionState(
        defaultLabel = "  #  ",  // Two leading and trailing spaces
        chosen = false
    )

    // Create individual chips for each selected handle/tag
    val handleFilterChips = selectedHandleChips.map { account ->
        FilterState.SingleActionState(
            defaultLabel = account.text.removePrefix("@"),  // Remove leading @
            chosen = true
        )
    }

    val hashtagFilterChips = selectedTagChips.map { hashtag ->
        FilterState.SingleActionState(
            defaultLabel = hashtag.text,  // Keep leading # for tags
            chosen = true
        )
    }

    val favoriteFilter = FilterState.TripleState(
        defaultLabel = "Favorites",
        iconPainterInclude = Icons.Filled.Star,
        iconPainterExclude = Icons.Outlined.StarOutline,
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

    // Order: @ # Favorites DateChips HandleChips TagChips (last added appears rightmost)
    // Reverse the lists so newest additions appear rightmost
    return listOf(handleChip, tagChip) + listOf(favoriteFilter) + dateChips + handleFilterChips.reversed() + hashtagFilterChips.reversed()
}
