package cut.the.crap.ui.content.links

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Immutable
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.ui.components.ActiveState
import cut.the.crap.ui.components.DateType
import cut.the.crap.ui.components.FilterState
import cut.the.crap.ui.components.MyEditDialogStyle

@Immutable
data class LinksScreenState(
    val query: String = "",
    val editText: String = "",
    val focusedItem: ContentLink? = ContentLink(),
    val searchExpanded: Boolean = false,
    val textInputExpanded: Boolean = false,
    val showEditDialog: MyEditDialogStyle? = null,
    val showCommentQuoteDialog: ContentLink? = null,  // If not null, shows the comment/quote dialog for this link
    val showDateFilterSheet: Boolean = false,
    val selectedDateType: DateType = DateType.START,  // Track which date tab to show
    val startTime: Long? = null,
    val endTime: Long? = null,
    val checkMarks: Boolean = false,
    val selectedItems: List<Int> = listOf(),
    val hiddenFilters: List<String> = emptyList(),  // Hidden keyword filters from long-press
    val showHandleSelectionDialog: Boolean = false,
    val handleList: List<cut.the.crap.data.domain.KeyWord> = emptyList(),
    val selectedHandles: Set<Int> = emptySet(),
    val tagList: List<cut.the.crap.data.domain.KeyWord> = emptyList(),
    val keyWordList: List<cut.the.crap.data.domain.KeyWord> = emptyList(),
    val showKeywordSelectionDialog: Boolean = false,
    val currentEditingLink: ContentLink? = null,
    val currentKeywordType: cut.the.crap.ui.components.api.ChipsType = cut.the.crap.ui.components.api.ChipsType.KeyWords,
    val filterStateList: List<FilterState> = listOf(
        FilterState.TripleState(
            defaultLabel = "Favourites",
            iconPainterInclude = Icons.Filled.Favorite,
            iconPainterExclude = Icons.Outlined.FavoriteBorder,
            activeState = ActiveState.Default
        ),
        FilterState.DateState(
            defaultLabel = "Date Range",
            dateType = DateType.START,
            date = null
        ),
    ),
)