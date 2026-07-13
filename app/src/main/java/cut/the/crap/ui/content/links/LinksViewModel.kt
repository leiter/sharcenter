package cut.the.crap.ui.content.links

import cut.the.crap.platform.FileAccess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cut.the.crap.data.db.ItemManager
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.domain.ContentLinkRepository
import cut.the.crap.data.domain.KeyWord
import cut.the.crap.data.domain.KeywordType
import cut.the.crap.data.preferences.SettingsRepository
import cut.the.crap.data.rest.Message
import cut.the.crap.data.rest.MessageRepository
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.YouTubeRepository
import cut.the.crap.data.rest.YouTubeUrlParser
import cut.the.crap.data.rest.task.JobQueueRepository
import cut.the.crap.data.rest.task.ShareLinksTask
import cut.the.crap.tools.DescriptionParser
import cut.the.crap.tools.LinkMetadata
import cut.the.crap.tools.isBlueskyUrl
import cut.the.crap.tools.isMastodonUrl
import cut.the.crap.tools.isRedditUrl
import cut.the.crap.tools.isTikTokUrl
import cut.the.crap.tools.parseSocialMediaUrl
import cut.the.crap.tools.prepareUrlInformation
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.components.api.ContentLinkAction
import cut.the.crap.ui.components.api.FileAction
import cut.the.crap.ui.components.api.KeywordAction
import cut.the.crap.ui.components.api.ListAction
import cut.the.crap.ui.components.api.TextAction
import cut.the.crap.ui.components.api.UiAction
import cut.the.crap.platform.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LinksViewModel constructor(
    internal val contentRepository: ContentLinkRepository,
    private val repository: MessageRepository,
    internal val fileAccess: FileAccess,
    private val settingsRepository: SettingsRepository,
    internal val keywordRepository: cut.the.crap.data.domain.KeywordRepository,
    internal val jobQueueRepository: JobQueueRepository,
    internal val youTubeRepository: YouTubeRepository,
    // Injected rather than hardcoded, for two reasons: `Dispatchers.IO` does not exist in
    // commonMain (WP6 moves this class there), and hardcoding `Dispatchers.Default` races
    // `advanceUntilIdle` in tests, which made the suite intermittently flaky.
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    internal val itemManager = ItemManager(contentRepository)

    internal val internalScreenState = MutableStateFlow(LinksScreenState())

    init {
        // Load and apply defaults from settings (only on initial load)
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.firstOrNull()
            if (settings != null) {
                // Apply default date range
                val preset = settings.linksDateRangePreset
                val startTime = preset.toStartTimestamp()
                val endTime = preset.toEndTimestamp()

                // Apply default favorite filter
                val favoriteFilterValue = settings.linksFavoriteFilterPreset.toFilterValue()
                val favoriteActiveState = settings.linksFavoriteFilterPreset.toActiveState()

                // Apply default sort order
                if (settings.linksSortOrderPreset.isSortByDate()) {
                    itemManager.sortByDate()
                } else {
                    itemManager.sortByListPosition()
                }

                // Apply filters
                itemManager.updateFilterState(
                    mapOf(
                        "timeFrameStart" to startTime,
                        "timeFrameEnd" to endTime,
                        "includeFavourite" to favoriteFilterValue
                    )
                )

                // Update filter chips to reflect the defaults
                val updatedFilterChips = internalScreenState.value.filterStateList.buildDateFilterChips(
                    startTime = startTime,
                    endTime = endTime,
                    favoriteActiveState = favoriteActiveState
                )

                // Update screen state to reflect the defaults
                internalScreenState.value = internalScreenState.value.copy(
                    startTime = startTime,
                    endTime = endTime,
                    filterStateList = updatedFilterChips
                )
            }
        }
    }

    // handleList for the "Select Accounts" dialog: usernames derived from existing links
    // merged with saved handles from the ACCOUNT keyword pool (e.g. shared X profiles).
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private val handleListFlow: StateFlow<List<KeyWord>> =
        combine(
            // Wait 300ms after last emission to reduce redundant processing
            itemManager.filteredAndSortedItems.debounce(300),
            keywordRepository.getByType(KeywordType.ACCOUNT)
        ) { links, savedHandles -> links to savedHandles }
            .map { (links, savedHandles) ->
                // Process URLs in background thread to avoid blocking UI
                withContext(defaultDispatcher) {
                    // Extract usernames from all links
                    // For YouTube: prefer channel name from description metadata
                    // For others: extract from URL path
                    val derived = links
                        .asSequence()
                        .map { link ->
                            if (YouTubeUrlParser.isYouTubeUrl(link.link)) {
                                LinkMetadata.getChannelName(link) ?: ""
                            } else if (isBlueskyUrl(link.link)) {
                                LinkMetadata.getBlueskyAuthor(link)
                                    ?: parseSocialMediaUrl(link.link)?.username ?: ""
                            } else if (isMastodonUrl(link.link)) {
                                LinkMetadata.getMastodonAuthor(link)
                                    ?: parseSocialMediaUrl(link.link)?.username ?: ""
                            } else if (isTikTokUrl(link.link)) {
                                LinkMetadata.getTikTokAuthor(link)
                                    ?: parseSocialMediaUrl(link.link)?.username ?: ""
                            } else if (isRedditUrl(link.link)) {
                                LinkMetadata.getRedditAuthor(link)
                                    ?: parseSocialMediaUrl(link.link)?.username ?: ""
                            } else {
                                val urlInfo = prepareUrlInformation(link.link)
                                urlInfo.getOrNull(1) ?: ""
                            }
                        }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .toList()

                    // Merge link-derived usernames with saved handles, collapsing
                    // duplicates case-insensitively and ignoring a leading "@" (derived
                    // usernames have no prefix, saved handles are stored as "@name").
                    // Link-derived entries win so their un-prefixed form still matches
                    // link URLs when used as a filter in ConfirmHandleSelection.
                    val byKey = LinkedHashMap<String, KeyWord>()
                    derived.forEach { username ->
                        byKey.getOrPut(username.removePrefix("@").lowercase()) {
                            KeyWord(text = username, type = KeywordType.ACCOUNT)
                        }
                    }
                    savedHandles.forEach { handle ->
                        byKey.getOrPut(handle.text.removePrefix("@").lowercase()) { handle }
                    }

                    // Sort alphabetically and assign sequential ids for stable dialog keys.
                    byKey.values
                        .sortedBy { it.text.removePrefix("@").lowercase() }
                        .mapIndexed { index, keyWord -> keyWord.copy(id = index) }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )

    // Load tags from database
    private val tagListFlow: StateFlow<List<KeyWord>> =
        keywordRepository.getByType(KeywordType.HASHTAG)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )

    // Load keywords from database
    private val keyWordListFlow: StateFlow<List<KeyWord>> =
        keywordRepository.getByType(KeywordType.TAG)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )

    // Combine _screenState with linkSubstring and handleList to keep everything in sync
    val screenState: StateFlow<LinksScreenState> = combine(
        internalScreenState,
        itemManager.linkSubstring,
        handleListFlow,
        tagListFlow,
        keyWordListFlow
    ) { state, query, handleList, tagList, keyWordList ->
        state.copy(
            query = query,
            handleList = handleList,
            tagList = tagList,
            keyWordList = keyWordList
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = internalScreenState.value
    )

    private val _snackBarMessage = MutableSharedFlow<LinksSnackbar>()
    val snackBarMessage: SharedFlow<LinksSnackbar> = _snackBarMessage.asSharedFlow()

    // Action handlers report *what happened*; the UI phrases it (see LinksSnackbarMessages).
    internal suspend fun emitSnackBarMessage(event: LinksSnackbar) {
        _snackBarMessage.emit(event)
    }

    val listState : StateFlow<List<ContentLink>> =
        combine(
            itemManager.filteredAndSortedItems,
            internalScreenState
        ) { items, screenState ->
            // Apply hidden filters on top of database-filtered results
            val hiddenFilters = screenState.hiddenFilters
            if (hiddenFilters.isEmpty()) {
                items
            } else {
                // Separate username filters from domain filters (cache this outside the filter)
                // Usernames: no dots, length > 3 (OR logic)
                // Domains: short (<=3 chars) or contains dots (AND logic)
                val (usernameFilters, domainFilters) = hiddenFilters.partition {
                    !it.contains(".") && it.length > 3
                }

                // Use asSequence for lazy evaluation
                items.asSequence()
                    .filter { contentLink ->
                        // Username filters use OR logic (match ANY username)
                        val usernameMatch = if (usernameFilters.isEmpty()) {
                            true
                        } else {
                            usernameFilters.any { username ->
                                matchesFilter(contentLink, username)
                            }
                        }

                        // Domain filters use AND logic (match ALL domains)
                        val domainMatch = domainFilters.all { domain ->
                            matchesFilter(contentLink, domain)
                        }

                        // Both conditions must be satisfied
                        usernameMatch && domainMatch
                    }
                    .toList()
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalCount: StateFlow<Int> =
        contentRepository.getItems(
            includeFavourite = null,
            includeHidden = null,
            linkSubstring = null,
            sortByListPosition = false,
            sortByDate = false,
            startTime = null,
            endTime = null
        ).map { it.size }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )


    private val _response = MutableStateFlow("")
    val response: StateFlow<String> = _response

    fun sendMessage(text: String) {
        viewModelScope.launch {
            val result = repository.postMessage(Message(text))
            _response.value = result.getOrNull() ?: ""
        }
    }

    fun insertContentLink(contentLink: ContentLink) {
        viewModelScope.launch {
            async { contentRepository.insert(contentLink) }.await()
            itemManager.triggerReload()

            // Fetch YouTube metadata in background
            if (YouTubeUrlParser.isYouTubeUrl(contentLink.link)) {
                Log.d("YT_META", "Detected YouTube URL: ${contentLink.link}")
                launch(ioDispatcher) {
                    val result = youTubeRepository.getVideoMetadata(contentLink.link)
                    Log.d("YT_META", "oEmbed result: $result")
                    if (result is Result.Success) {
                        val recentItems = contentRepository.byTimeRange(
                            start = contentLink.added - 1000,
                            end = contentLink.added + 1000
                        )
                        Log.d("YT_META", "byTimeRange found ${recentItems.size} items, looking for: ${contentLink.link}")
                        val dbItem = recentItems.firstOrNull { it.link == contentLink.link }
                        if (dbItem == null) {
                            Log.e("YT_META", "Could not find inserted item in DB!")
                            return@launch
                        }
                        Log.d("YT_META", "Found dbItem id=${dbItem.id}, updating with channel=${result.data.channelName}")
                        val type = parseSocialMediaUrl(contentLink.link)?.contentType ?: "video"
                        val updated = LinkMetadata.setYouTubeMetadata(
                            dbItem,
                            channelName = result.data.channelName,
                            videoTitle = result.data.title,
                            thumbnailUrl = result.data.thumbnailUrl,
                            contentType = type
                        )
                        Log.d("YT_META", "New description: ${updated.description}")
                        contentRepository.update(updated)
                        Log.d("YT_META", "Update complete")
                    } else if (result is Result.Error) {
                        Log.e("YT_META", "oEmbed failed: ${result.error.debugText}", result.exception)
                    }
                }
            }
        }
    }
    internal fun updateContentLink(contentLink: ContentLink) {
        viewModelScope.launch { contentRepository.update(contentLink) } }

    /**
     * Check if a ContentLink matches a filter keyword.
     * For short keywords (likely domains like "x", "fb"), use domain-specific matching.
     * For longer keywords, use substring matching against parsed description fields.
     */
    private fun matchesFilter(contentLink: ContentLink, keyword: String): Boolean {
        val link = contentLink.link.lowercase()
        val keywordLower = keyword.lowercase()

        // Parse the structured description to search in metadata + tag categories
        val parsed = DescriptionParser.parse(contentLink.description)

        // For very short keywords (1-3 chars), assume it's a domain/platform token (e.g. "x", "fb"
        // from long-pressing a platform icon) and match precisely. A loose substring match is
        // meaningless here: the letter "x" appears inside almost any video title, thumbnail URL, or
        // channel name, which would wrongly keep every YouTube item when filtering to "x". So match
        // on the URL domain, plus discrete tag tokens (handles/hashtags/keywords) that equal the
        // keyword exactly — never a substring of the free-text metadata.
        if (keywordLower.length <= 3) {
            val domainPattern = Regex("""://(?:www\.)?([^/]+)""")
            val domain = domainPattern.find(link)?.groupValues?.get(1)?.lowercase() ?: ""
            val domainMatch = domain.split(".").any { it == keywordLower }

            val tagMatch = (parsed.handles + parsed.hashtags + parsed.keywords)
                .any { it.lowercase() == keywordLower }

            return domainMatch || tagMatch
        }

        // For longer keywords, use substring matching across the link and all description fields
        val allSearchable = parsed.metadata + parsed.handles + parsed.hashtags + parsed.keywords
        val descriptionMatch = allSearchable.any { it.lowercase().contains(keywordLower) }
        return link.contains(keywordLower) || descriptionMatch
    }

    /**
     * Main action dispatcher - delegates to specific action handlers
     */
    fun consumeAction(action: Action) {
        when (action) {
            is ContentLinkAction -> handleContentLinkAction(action)
            is TextAction -> handleTextAction(action)
            is UiAction -> handleUiAction(action)
            is KeywordAction -> handleKeywordAction(action)
            is ListAction -> handleListAction(action)
            is FileAction -> handleFileAction(action)
            // Actions not handled by this ViewModel
            else -> Unit
        }
    }
}
