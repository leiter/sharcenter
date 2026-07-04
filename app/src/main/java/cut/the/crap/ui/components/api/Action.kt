package cut.the.crap.ui.components.api

import android.net.Uri
import cut.the.crap.data.domain.ContentItem
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.tools.TextValueWrapper
import cut.the.crap.ui.components.MyEditDialogStyle

/**
 * Type-safe action sealed interface hierarchy
 * Eliminates unsafe casts by using specific types for each action domain
 */
sealed interface Action

// ========== Content Link Actions ==========
// Actions that operate on ContentLink entities
sealed interface ContentLinkAction : Action {
    val item: ContentLink

    data class Delete(override val item: ContentLink) : ContentLinkAction
    data class OfferDelete(override val item: ContentLink) : ContentLinkAction
    data class ToggleFavourite(override val item: ContentLink) : ContentLinkAction
    data class ToggleSelection(override val item: ContentLink) : ContentLinkAction
    data class EnterSelectionMode(override val item: ContentLink) : ContentLinkAction
    data class EditSearchHint(
        override val item: ContentLink,
        val searchHint: String
    ) : ContentLinkAction
    data class Open(override val item: ContentLink) : ContentLinkAction
    data class CopyToClipboard(override val item: ContentLink) : ContentLinkAction
    data class ShowCommentQuoteDialog(override val item: ContentLink) : ContentLinkAction
    data class CreateComment(override val item: ContentLink, val comment: String) : ContentLinkAction
    data class CreateQuote(override val item: ContentLink, val quote: String) : ContentLinkAction
    data class ManageKeywords(override val item: ContentLink, val type: ChipsType) : ContentLinkAction
}

// ========== Text/String Actions ==========
// Actions that operate on text and strings
sealed interface TextAction : Action {
    data class EditContentText(val value: TextValueWrapper) : TextAction
    data class EditQueryText(val query: String) : TextAction
    data class InsertText(val text: String) : TextAction
    data class PostQuery(val query: String) : TextAction
    data class AddHiddenFilter(val keyword: String) : TextAction
    data class RemoveHiddenFilter(val keyword: String) : TextAction
    data object ClearHiddenFilters : TextAction
    data class SetStartDateFilter(val timestamp: Long, val screen: Screen) : TextAction
    data object PasteFromClipboard : TextAction
    data object CopyContentText : TextAction
    data object ClearContentText : TextAction
}

// ========== UI State Actions ==========
// Actions that control UI state
sealed interface UiAction : Action {
    data class ExpandSearch(val expanded: Boolean, val screen: Screen = Screen.Current) : UiAction
    data class ExpandTextInput(val expanded: Boolean, val screen: Screen = Screen.Current) : UiAction
    data class ShowEditDialog(val dialog: MyEditDialogStyle?, val screen: Screen = Screen.Current) : UiAction
    data class ShowChips(val type: ChipsType, val screen: Screen = Screen.Current, val source: String = "filter") : UiAction
    data class ChipClicked(val index: Int, val type: ChipsType = ChipsType.Filter, val screen: Screen = Screen.Current) : UiAction
    data class ShowKeywordSelectionDialog(val type: ChipsType, val show: Boolean, val screen: Screen = Screen.Current) : UiAction
    data class ShowDateFilterSheet(val show: Boolean, val screen: Screen = Screen.Current) : UiAction
    data class SetDateFilter(val startTime: Long?, val endTime: Long?, val screen: Screen = Screen.Current) : UiAction
    data class ClearDateFilter(val dateType: cut.the.crap.ui.components.DateType, val screen: Screen = Screen.Current) : UiAction
    data class ExitSelectionMode(val screen: Screen = Screen.Current) : UiAction
}

enum class Screen {
    Current,  // Used when action should apply to current screen only
    Posts,    // PrepareTweet screen
    Links     // Room screen
}

// ========== Content Item Actions ==========
// Actions for managing content items
sealed interface ContentItemAction : Action {
    data object CreateNew : ContentItemAction
    data class Load(val id: Int) : ContentItemAction
    data class Delete(val id: Int) : ContentItemAction
    data class ToggleFavorite(val id: Int) : ContentItemAction
    data class CopyToClipboard(val contentItem: ContentItem) : ContentItemAction
    data class PostOnTwitter(val contentItem: ContentItem) : ContentItemAction
    data class PostOnFacebook(val contentItem: ContentItem) : ContentItemAction
    data class ShareViaSheet(val contentItem: ContentItem) : ContentItemAction
}

// ========== Handle/Tag/KeyWords Actions ==========
// Actions for managing handles, tags, and keywords
sealed interface KeywordAction : Action {
    data class AddHandle(val text: String) : KeywordAction
    data class AddTag(val text: String) : KeywordAction
    data class AddKeyWord(val text: String) : KeywordAction
    data class ToggleHandleSelection(val handleId: Int) : KeywordAction
    data class ToggleTagSelection(val tagId: Int) : KeywordAction
    data class ToggleKeyWordSelection(val keyWordId: Int) : KeywordAction
    data object ConfirmHandleSelection : KeywordAction
    data object ConfirmTagSelection : KeywordAction
    data object ConfirmKeyWordSelection : KeywordAction
    data class DeleteHandle(val handleId: Int) : KeywordAction
    data class DeleteTag(val tagId: Int) : KeywordAction
    data class DeleteKeyWord(val keyWordId: Int) : KeywordAction
    data class DeleteHandlesBulk(val handleIds: List<Int>) : KeywordAction
    data class DeleteTagsBulk(val tagIds: List<Int>) : KeywordAction
    data class DeleteKeyWordsBulk(val keyWordIds: List<Int>) : KeywordAction
    data class ToggleFavorite(val id: Int) : KeywordAction
}

// ========== List Management Actions ==========
// Actions that manage lists
sealed interface ListAction : Action {
    data object DeleteAll : ListAction
    data object InvertList : ListAction
    data object ScrollToTop : ListAction
    data object SelectAll : ListAction
    data object DeselectAll : ListAction
    data object ToggleFavoritesForSelected : ListAction
    data object FireJob : ListAction
}

// ========== File Actions ==========
// Actions related to file operations
sealed interface FileAction : Action {
    data class Import(val uri: Uri) : FileAction
    data class Export(val outputStream: java.io.OutputStream?) : FileAction
    data object BackupDatabase : FileAction
    data class RestoreDatabase(val uri: Uri) : FileAction
}

// ========== Upload Actions ==========
// Actions related to file upload operations
sealed interface UploadAction : Action {
    data class SelectFiles(val uris: List<Uri>) : UploadAction
    data object ClearSelectedFiles : UploadAction
    data object StartUpload : UploadAction
}

// ========== Legacy/Unused Actions ==========
// These may need to be categorized or removed
sealed interface MiscAction : Action {
    data class SelectPressed(val item: ContentLink) : MiscAction
}

sealed interface ChipsType {
    data object Handle: ChipsType
    data object Tag: ChipsType
    data object KeyWords: ChipsType
    data object Filter: ChipsType
}