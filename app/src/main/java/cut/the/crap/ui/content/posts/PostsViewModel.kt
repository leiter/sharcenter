package cut.the.crap.ui.content.posts

import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cut.the.crap.data.db.ContentItemManager
import cut.the.crap.data.domain.ContentItemRepository
import cut.the.crap.data.domain.KeywordRepository
import cut.the.crap.data.domain.KeywordType
import cut.the.crap.tools.TextValueWrapper
import cut.the.crap.ui.components.FilterState
import cut.the.crap.data.rest.task.JobQueueRepository
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.components.api.ContentItemAction
import cut.the.crap.ui.components.api.KeywordAction
import cut.the.crap.ui.components.api.TextAction
import cut.the.crap.ui.components.api.UiAction
import cut.the.crap.ui.components.api.UploadAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FabState {
    data object Default : FabState
    data object Extended : FabState
}

/**
 * One-shot UI events the Posts screen consumes to show a snackbar.
 */
sealed interface PostsSnackbarEvent {
    /**
     * The editor was cleared and detached from [itemId], which remains in the list.
     * Offer the user the option to also delete that post.
     */
    data class OfferDeleteClearedItem(val itemId: Int) : PostsSnackbarEvent
}

@HiltViewModel
class PostsViewModel @Inject constructor(
    internal val keywordRepository: KeywordRepository,
    internal val contentItemRepository: ContentItemRepository,
    private val settingsRepository: cut.the.crap.data.preferences.SettingsRepository,
    internal val jobQueueRepository: JobQueueRepository
) : ViewModel() {

    internal val internalScreenState = MutableStateFlow(PostsScreenState())

    internal val contentItemManager = ContentItemManager(contentItemRepository)

    // Load handles from database
    private val accounts = keywordRepository.getByType(KeywordType.ACCOUNT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Load tags from database
    private val hashtags = keywordRepository.getByType(KeywordType.HASHTAG)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Load keywords from database
    private val tags = keywordRepository.getByType(KeywordType.TAG)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Load content items from database
    val contentItems = contentItemManager.filteredAndSortedItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val screenState: StateFlow<PostsScreenState> = internalScreenState

    // One-shot snackbar events for the Posts screen
    private val _snackBarEvents = MutableSharedFlow<PostsSnackbarEvent>()
    val snackBarEvents: SharedFlow<PostsSnackbarEvent> = _snackBarEvents.asSharedFlow()

    // Helper for action handlers (in other files) to emit snackbar events
    internal suspend fun emitSnackBarEvent(event: PostsSnackbarEvent) {
        _snackBarEvents.emit(event)
    }

    internal var activeItemId: Int? = null

    init {
        // Initialize filter state list with @ and # chips
        internalScreenState.value = internalScreenState.value.copy(
            filterStateList = emptyList<FilterState>()
                .buildDateFilterChips(null, null, emptyList(), emptyList())
        )

        // Load and apply defaults from settings (only on initial load)
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.firstOrNull()
            if (settings != null) {
                // Apply default date range
                val preset = settings.postsDateRangePreset
                val startTime = preset.toStartTimestamp()
                val endTime = preset.toEndTimestamp()

                // Apply default favorite filter
                val favoriteFilterValue = settings.postsFavoriteFilterPreset.toFilterValue()
                val favoriteActiveState = settings.postsFavoriteFilterPreset.toActiveState()
                contentItemManager.filterByFavorite(favoriteFilterValue)

                // Apply default sort order
                if (settings.postsSortOrderPreset.isSortByDate()) {
                    contentItemManager.sortByDate()
                } else {
                    contentItemManager.sortByOrder()
                }

                // Only apply date range if we have a valid time range (not ALL_TIME)
                if (startTime != null || endTime != null) {
                    contentItemManager.filterByTimeFrame(startTime, endTime)
                }

                // Update filter chips to reflect the defaults
                val updatedFilters = internalScreenState.value.filterStateList.buildDateFilterChips(
                    startTime,
                    endTime,
                    internalScreenState.value.selectedHandleChips,
                    internalScreenState.value.selectedTagChips,
                    favoriteActiveState
                )

                internalScreenState.value = internalScreenState.value.copy(
                    startTime = startTime,
                    endTime = endTime,
                    filterStateList = updatedFilters
                )
            }
        }

        // Update screen state when handles, tags, or keywords change
        viewModelScope.launch {
            accounts.collect { handleList ->
                internalScreenState.value = internalScreenState.value.copy(handleList = handleList)
            }
        }
        viewModelScope.launch {
            hashtags.collect { tagList ->
                internalScreenState.value = internalScreenState.value.copy(tagList = tagList)
            }
        }
        viewModelScope.launch {
            tags.collect { keyWordsList ->
                internalScreenState.value = internalScreenState.value.copy(keyWordsList = keyWordsList)
            }
        }
        // Load active item on init
        viewModelScope.launch {
            val activeItem = contentItemRepository.getActiveItem()
            if (activeItem != null) {
                activeItemId = activeItem.id
                internalScreenState.value = internalScreenState.value.copy(
                    focusedContentText = TextValueWrapper(
                        newText = activeItem.text,
                        selection = Pair(activeItem.text.length, activeItem.text.length)
                    )
                )
            }
        }
    }

    fun consumeAction(action: Action) {
        when (action) {
            is TextAction -> handleTextAction(action)
            is UiAction -> handleUiAction(action)
            is KeywordAction -> handleKeywordAction(action)
            is ContentItemAction -> handleContentItemAction(action)
            // Actions not handled by this ViewModel
            else -> Unit
        }
    }

    fun consumeActionWithResolver(action: Action, contentResolver: ContentResolver) {
        when (action) {
            is UploadAction -> handleUploadAction(action, contentResolver)
            else -> consumeAction(action)
        }
    }

}