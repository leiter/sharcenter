package cut.the.crap.ui.components.api

import cut.the.crap.platform.PlatformUri

import cut.the.crap.data.domain.ContentItem
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.rest.campaign.CampaignPost
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
    /** Open the channel / user profile URL derived from this link. */
    data class OpenProfile(override val item: ContentLink, val url: String) : ContentLinkAction
    data class CopyToClipboard(override val item: ContentLink) : ContentLinkAction

    /** Seed the Posts editor with this link (URL + its saved handles/hashtags/keywords) and switch to it. */
    data class ComposePost(override val item: ContentLink) : ContentLinkAction
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

    // Editor-scoped overflow actions that operate on the current draft text
    data object PostContentOnTwitter : TextAction
    data object PostContentOnFacebook : TextAction
    data object ShareContentViaSheet : TextAction
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
    /** Open (non-null [type]) or close (null) the bulk-tag dialog for the current selection. */
    data class ShowBulkTagDialog(val type: ChipsType?, val screen: Screen = Screen.Current) : UiAction
    data class SetDateFilter(val startTime: Long?, val endTime: Long?, val screen: Screen = Screen.Current) : UiAction
    data class ClearDateFilter(val dateType: cut.the.crap.ui.components.DateType, val screen: Screen = Screen.Current) : UiAction
    data class ExitSelectionMode(val screen: Screen = Screen.Current) : UiAction
}

// ========== Campaign Post Actions ==========
// Actions on a single CampaignPost (per country/language, ready-to-publish text). Handled locally
// by CampaignPostComposerScreen rather than the app-wide handleAction router — see Action.kt at
// the top of the hierarchy for why (campaigns are not wired into App.kt's action lambda).
sealed interface CampaignPostAction : Action {
    data class PostOnTwitter(val post: CampaignPost) : CampaignPostAction
    data class PostOnFacebook(val post: CampaignPost) : CampaignPostAction
    data class ShareViaSheet(val post: CampaignPost) : CampaignPostAction
    /** [key] is a pre-built composite key — see [cut.the.crap.data.preferences.campaignPostHideKey]. */
    data class Hide(val key: String) : CampaignPostAction
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

    // Selection / batch actions (Posts screen). Kept on ContentItemAction so only the
    // PostsViewModel reacts — the screen-agnostic ListAction variants would double-dispatch
    // to the LinksViewModel. Exit uses the screen-scoped UiAction.ExitSelectionMode.
    data class EnterSelectionMode(val id: Int) : ContentItemAction
    data class ToggleSelection(val id: Int) : ContentItemAction
    data object SelectAll : ContentItemAction
    data object DeselectAll : ContentItemAction
    data object DeleteSelected : ContentItemAction
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
    /** Delete only the currently selected items (leaves unselected items untouched). */
    data object DeleteSelected : ListAction
    /** Add [tags] of the given [type] to every currently selected item. */
    data class TagSelected(val type: ChipsType, val tags: List<String>) : ListAction
    data object FireJob : ListAction
}

// ========== File Actions ==========
// Actions related to file operations
sealed interface FileAction : Action {
    data class Import(val uri: PlatformUri) : FileAction

    /**
     * Export the selected links. Carries no payload: it used to hold a `java.io.OutputStream`,
     * which meant the screen had to intercept this action, open a stream and re-dispatch it — and
     * it put a JVM type in the middle of the action contract. The ViewModel now names the file and
     * hands the text to [cut.the.crap.platform.FileAccess], which knows where Downloads is.
     */
    data object Export : FileAction
    data object BackupDatabase : FileAction
    data class RestoreDatabase(val uri: PlatformUri) : FileAction
}

// ========== Upload Actions ==========
// Actions related to file upload operations
sealed interface UploadAction : Action {
    data class SelectFiles(val uris: List<PlatformUri>) : UploadAction
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