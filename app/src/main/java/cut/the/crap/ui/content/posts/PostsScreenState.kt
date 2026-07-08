package cut.the.crap.ui.content.posts

import android.net.Uri
import androidx.compose.runtime.Immutable
import cut.the.crap.data.domain.KeyWord
import cut.the.crap.tools.TextValueWrapper
import cut.the.crap.ui.components.DateType
import cut.the.crap.ui.components.FilterState

/**
 * Tracks the source context of handle/tag/keyword dialog
 */
enum class DialogSource {
    EDITOR,  // Opened from ContentEditor - insert into text
    FILTER   // Opened from Filter section - add as filter chip
}

@Immutable
data class PostsScreenState(
    val focusedContentText: TextValueWrapper = TextValueWrapper(),
    val fabState: FabState = FabState.Default,
    val query: String = "",
    val searchExpanded: Boolean = false,
    val showHandleSelectionDialog: Boolean = false,
    val handleList: List<KeyWord> = emptyList(),
    val selectedHandles: Set<Int> = emptySet(),
    val showTagSelectionDialog: Boolean = false,
    val tagList: List<KeyWord> = emptyList(),
    val selectedTags: Set<Int> = emptySet(),
    val showKeyWordsSelectionDialog: Boolean = false,
    val keyWordsList: List<KeyWord> = emptyList(),
    val selectedKeyWords: Set<Int> = emptySet(),
    val contentList: List<String> = emptyList(),
    val filterExpanded: Boolean = false,
    val filterStateList: List<FilterState> = emptyList(),
    val showDateFilterSheet: Boolean = false,
    val selectedDateType: DateType = DateType.START,  // Track which date tab to show
    val startTime: Long? = null,
    val endTime: Long? = null,
    val selectedHandleChips: List<KeyWord> = emptyList(),  // Active handle filter chips
    val selectedTagChips: List<KeyWord> = emptyList(),  // Active tag filter chips
    val dialogSource: DialogSource = DialogSource.FILTER,  // Track where dialog was opened from
    val selectedFileUris: List<Uri> = emptyList(),  // Files selected for upload
    val isUploading: Boolean = false,  // Upload in progress
    val selectionMode: Boolean = false,  // Batch selection mode active
    val selectedItems: List<Int> = emptyList()  // IDs of items selected in batch mode
)