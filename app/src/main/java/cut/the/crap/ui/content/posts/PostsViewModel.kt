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
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.eci.EciStatistics
import cut.the.crap.data.rest.eci.EciStatisticsRepository
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

/**
 * One-shot events emitted while loading the European Citizens' Initiative statistics
 * table from the top bar's key button.
 */
sealed interface EciUiEvent {
    /** Statistics loaded and parsed successfully — navigate to the table screen. */
    data object NavigateToTable : EciUiEvent

    /** Loading failed — show [message] in a toast. */
    data class ShowError(val message: String) : EciUiEvent
}

@HiltViewModel
class PostsViewModel @Inject constructor(
    internal val keywordRepository: KeywordRepository,
    internal val contentItemRepository: ContentItemRepository,
    private val settingsRepository: cut.the.crap.data.preferences.SettingsRepository,
    internal val jobQueueRepository: JobQueueRepository,
    private val eciStatisticsRepository: EciStatisticsRepository
) : ViewModel() {

    companion object {
        /** The European Citizens' Initiative whose statistics the key button loads. */
        private const val ECI_INITIATIVE_URL =
            "https://citizens-initiative.europa.eu/initiatives/details/2025/000005_en"
    }

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

    // ECI statistics: loading flag, last loaded result, and one-shot nav/error events.
    private val _eciLoading = MutableStateFlow(false)
    val eciLoading: StateFlow<Boolean> = _eciLoading

    private val _eciStatistics = MutableStateFlow<EciStatistics?>(null)
    val eciStatistics: StateFlow<EciStatistics?> = _eciStatistics

    private val _eciEvents = MutableSharedFlow<EciUiEvent>()
    val eciEvents: SharedFlow<EciUiEvent> = _eciEvents.asSharedFlow()

    /**
     * Loads and parses the ECI statistics table. Toggles [eciLoading] for the button's
     * spinner, then emits [EciUiEvent.NavigateToTable] on success or
     * [EciUiEvent.ShowError] on failure. Ignores taps while a load is in flight.
     */
    fun loadEciStatistics() {
        if (_eciLoading.value) return
        _eciLoading.value = true
        viewModelScope.launch {
            when (val result = eciStatisticsRepository.getStatistics(ECI_INITIATIVE_URL)) {
                is Result.Success -> {
                    _eciStatistics.value = result.data
                    _eciEvents.emit(EciUiEvent.NavigateToTable)
                }
                is Result.Error -> {
                    _eciEvents.emit(EciUiEvent.ShowError(result.message))
                }
            }
            _eciLoading.value = false
        }
    }

    /**
     * Inserts each generated motivational post as a draft (inactive) ContentItem so they
     * appear in the Posts list for review before sending. [onDone] is invoked on the main
     * dispatcher once all drafts are persisted.
     */
    fun createDraftPosts(texts: List<String>, onDone: () -> Unit = {}) {
        val drafts = texts.filter { it.isNotBlank() }
        if (drafts.isEmpty()) {
            onDone()
            return
        }
        viewModelScope.launch {
            drafts.forEach { text ->
                contentItemRepository.insert(
                    cut.the.crap.data.domain.ContentItem(text = text, isActive = false)
                )
            }
            onDone()
        }
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