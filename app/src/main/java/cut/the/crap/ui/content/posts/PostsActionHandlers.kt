package cut.the.crap.ui.content.posts

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import cut.the.crap.platform.Log
import androidx.lifecycle.viewModelScope
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.task.FileUploadData
import cut.the.crap.tools.TextValueWrapper
import cut.the.crap.tools.insertText
import cut.the.crap.tools.isForThisScreen
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import cut.the.crap.ui.components.DateType
import cut.the.crap.ui.components.FilterState
import cut.the.crap.ui.components.api.ChipsType
import cut.the.crap.ui.components.api.ContentItemAction
import cut.the.crap.ui.components.api.KeywordAction
import cut.the.crap.ui.components.api.TextAction
import cut.the.crap.ui.components.api.UiAction
import cut.the.crap.ui.components.api.UploadAction
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Extension functions for handling TextActions in PostsViewModel
 */
internal fun PostsViewModel.handleTextAction(action: TextAction) {
    when (action) {
        is TextAction.EditContentText -> {
            internalScreenState.update {
                it.copy(focusedContentText = action.value)
            }
            // Auto-save to database
            autoSaveContentItem(action.value)
        }

        is TextAction.EditQueryText -> {
            internalScreenState.update {
                it.copy(query = action.query)
            }
            // Apply the query to the content list so search actually filters results.
            contentItemManager.filterByQuery(action.query)
        }

        is TextAction.PasteFromClipboard -> {
            // This will be handled at the Activity level (needs ClipboardManager)
            // For now, do nothing - MainActivity will handle it
        }

        is TextAction.ClearContentText -> {
            // Leave the current post untouched in the list and detach from it,
            // then present a fresh empty post as the focused editable post.
            // A new DB item is only created once the user starts typing
            // (handled by autoSaveContentItem when activeItemId == null).
            val clearedItemId = activeItemId
            val hadContent = internalScreenState.value.focusedContentText.newText.isNotBlank()
            createNewContentItem()
            // Offer to also delete the post we just detached from, in case the
            // user wanted to discard it rather than keep it in the list.
            if (clearedItemId != null && hadContent) {
                viewModelScope.launch {
                    emitSnackBarEvent(PostsSnackbarEvent.OfferDeleteClearedItem(clearedItemId))
                }
            }
        }

        else -> Unit
    }
}

/**
 * Extension functions for handling UiActions in PostsViewModel
 */
internal fun PostsViewModel.handleUiAction(action: UiAction) {
    when (action) {
        is UiAction.ExpandSearch -> {
            if (!this.isForThisScreen(action.screen)) return
            internalScreenState.update {
                it.copy(searchExpanded = action.expanded)
            }
        }

        is UiAction.ExitSelectionMode -> {
            if (!this.isForThisScreen(action.screen)) return
            internalScreenState.update {
                it.copy(selectionMode = false, selectedItems = emptyList())
            }
        }

        is UiAction.ExpandTextInput -> {
            if (!this.isForThisScreen(action.screen)) return
            internalScreenState.update {
                it.copy(filterExpanded = action.expanded)
            }
        }

        is UiAction.ChipClicked -> {
            if (!this.isForThisScreen(action.screen)) return
            this.handleChipClick(action.index)
        }

        is UiAction.ShowDateFilterSheet -> {
            if (!this.isForThisScreen(action.screen)) return
            internalScreenState.update {
                it.copy(showDateFilterSheet = action.show)
            }
        }

        is UiAction.SetDateFilter -> {
            if (!this.isForThisScreen(action.screen)) return
            internalScreenState.update {
                contentItemManager.filterByTimeFrame(action.startTime, action.endTime)

                // Update filter state list to show appropriate date chips
                val updatedFilters = it.filterStateList.buildDateFilterChips(
                    action.startTime,
                    action.endTime,
                    it.selectedHandleChips,
                    it.selectedTagChips
                )

                it.copy(
                    startTime = action.startTime,
                    endTime = action.endTime,
                    filterStateList = updatedFilters
                )
            }
        }

        is UiAction.ClearDateFilter -> {
            if (!this.isForThisScreen(action.screen)) return
            internalScreenState.update {
                val newStart = if (action.dateType == DateType.START) null else it.startTime
                val newEnd = if (action.dateType == DateType.END) null else it.endTime

                contentItemManager.filterByTimeFrame(newStart, newEnd)

                // Update filter state list to show appropriate date chips
                val updatedFilters = it.filterStateList.buildDateFilterChips(
                    newStart,
                    newEnd,
                    it.selectedHandleChips,
                    it.selectedTagChips
                )

                it.copy(
                    startTime = newStart,
                    endTime = newEnd,
                    filterStateList = updatedFilters
                )
            }
        }

        is UiAction.ShowChips -> {
            if (!this.isForThisScreen(action.screen)) return
            val source = if (action.source == "editor") DialogSource.EDITOR else DialogSource.FILTER
            internalScreenState.update {
                when (action.type) {
                    is ChipsType.Handle -> it.copy(
                        showHandleSelectionDialog = it.showHandleSelectionDialog.not(),
                        selectedHandles = emptySet(), // Reset selection when opening
                        dialogSource = source
                    )
                    is ChipsType.Tag -> it.copy(
                        showTagSelectionDialog = it.showTagSelectionDialog.not(),
                        selectedTags = emptySet(), // Reset selection when opening
                        dialogSource = source
                    )
                    is ChipsType.Filter -> {
                        it
                    }
                    is ChipsType.KeyWords -> it.copy(
                        showKeyWordsSelectionDialog = it.showKeyWordsSelectionDialog.not(),
                        selectedKeyWords = emptySet(), // Reset selection when opening
                        dialogSource = source
                    )
                }
            }
        }

        is UiAction.ShowKeywordSelectionDialog -> {
            if (!this.isForThisScreen(action.screen)) return
            internalScreenState.update {
                when (action.type) {
                    ChipsType.Handle -> it.copy(
                        showHandleSelectionDialog = action.show,
                        selectedHandles = if (!action.show) emptySet() else it.selectedHandles
                    )
                    ChipsType.Tag -> it.copy(
                        showTagSelectionDialog = action.show,
                        selectedTags = if (!action.show) emptySet() else it.selectedTags
                    )
                    ChipsType.KeyWords -> it.copy(
                        showKeyWordsSelectionDialog = action.show,
                        selectedKeyWords = if (!action.show) emptySet() else it.selectedKeyWords
                    )
                    else -> it
                }
            }
        }

        else -> Unit
    }
}

/**
 * Extension functions for handling KeywordActions in PostsViewModel
 */
internal fun PostsViewModel.handleKeywordAction(action: KeywordAction) {
    when (action) {
        is KeywordAction.AddHandle -> {
            addAccount(action.text)
        }

        is KeywordAction.AddTag -> {
            addHashtag(action.text)
        }

        is KeywordAction.AddKeyWord -> {
            addTag(action.text)
        }

        is KeywordAction.ToggleHandleSelection -> internalScreenState.update {
            val currentSelection = it.selectedHandles.toMutableSet()
            if (currentSelection.contains(action.handleId)) {
                currentSelection.remove(action.handleId)
            } else {
                currentSelection.add(action.handleId)
            }
            it.copy(selectedHandles = currentSelection)
        }

        is KeywordAction.ToggleTagSelection -> internalScreenState.update {
            val currentSelection = it.selectedTags.toMutableSet()
            if (currentSelection.contains(action.tagId)) {
                currentSelection.remove(action.tagId)
            } else {
                currentSelection.add(action.tagId)
            }
            it.copy(selectedTags = currentSelection)
        }

        is KeywordAction.ToggleKeyWordSelection -> internalScreenState.update {
            val currentSelection = it.selectedKeyWords.toMutableSet()
            if (currentSelection.contains(action.keyWordId)) {
                currentSelection.remove(action.keyWordId)
            } else {
                currentSelection.add(action.keyWordId)
            }
            it.copy(selectedKeyWords = currentSelection)
        }

        is KeywordAction.ConfirmHandleSelection -> {
            confirmHandleSelection()
        }

        is KeywordAction.ConfirmTagSelection -> {
            confirmTagSelection()
        }

        is KeywordAction.ConfirmKeyWordSelection -> {
            confirmKeyWordSelection()
        }

        is KeywordAction.DeleteHandle -> {
            viewModelScope.launch {
                val item = keywordRepository.getById(action.handleId)
                item?.let { keywordRepository.delete(it) }
            }
        }

        is KeywordAction.DeleteTag -> {
            viewModelScope.launch {
                val item = keywordRepository.getById(action.tagId)
                item?.let { keywordRepository.delete(it) }
            }
        }

        is KeywordAction.DeleteKeyWord -> {
            viewModelScope.launch {
                val item = keywordRepository.getById(action.keyWordId)
                item?.let { keywordRepository.delete(it) }
            }
        }

        is KeywordAction.DeleteHandlesBulk -> {
            viewModelScope.launch {
                action.handleIds.forEach { id ->
                    val item = keywordRepository.getById(id)
                    item?.let { keywordRepository.delete(it) }
                }
            }
        }

        is KeywordAction.DeleteTagsBulk -> {
            viewModelScope.launch {
                action.tagIds.forEach { id ->
                    val item = keywordRepository.getById(id)
                    item?.let { keywordRepository.delete(it) }
                }
            }
        }

        is KeywordAction.DeleteKeyWordsBulk -> {
            viewModelScope.launch {
                action.keyWordIds.forEach { id ->
                    val item = keywordRepository.getById(id)
                    item?.let { keywordRepository.delete(it) }
                }
            }
        }

        is KeywordAction.ToggleFavorite -> {
            viewModelScope.launch {
                val item = keywordRepository.getById(action.id)
                item?.let {
                    keywordRepository.toggleFavorite(it.id, !it.isFavorite)
                }
            }
        }
    }
}

/**
 * Extension functions for handling ContentItemActions in PostsViewModel
 */
internal fun PostsViewModel.handleContentItemAction(action: ContentItemAction) {
    when (action) {
        is ContentItemAction.CreateNew -> createNewContentItem()

        is ContentItemAction.Load -> {
            viewModelScope.launch {
                val item = contentItemRepository.getById(action.id)
                item?.let { loadContentItem(it) }
            }
        }

        is ContentItemAction.Delete -> {
            viewModelScope.launch {
                val item = contentItemRepository.getById(action.id)
                item?.let { deleteContentItem(it) }
            }
        }

        is ContentItemAction.ToggleFavorite -> {
            viewModelScope.launch {
                val item = contentItemRepository.getById(action.id)
                item?.let { toggleContentItemFavorite(it) }
            }
        }

        is ContentItemAction.EnterSelectionMode -> internalScreenState.update {
            it.copy(selectionMode = true, selectedItems = listOf(action.id))
        }

        is ContentItemAction.ToggleSelection -> internalScreenState.update {
            val selected = it.selectedItems.toMutableList()
            if (selected.contains(action.id)) selected.remove(action.id) else selected.add(action.id)
            it.copy(selectedItems = selected)
        }

        is ContentItemAction.SelectAll -> internalScreenState.update {
            // "All" means every currently visible (filtered/sorted) item.
            it.copy(selectedItems = contentItems.value.map { item -> item.id })
        }

        is ContentItemAction.DeselectAll -> internalScreenState.update {
            it.copy(selectedItems = emptyList())
        }

        is ContentItemAction.DeleteSelected -> deleteSelectedContentItems()

        else -> Unit
    }
}

/**
 * Deletes every currently selected item in one pass, then leaves selection mode. If the item open
 * in the editor is among them, the editor is reset and the next remaining item (if any) is loaded —
 * handled once here rather than per-item to avoid concurrent active-item reloads.
 */
private fun PostsViewModel.deleteSelectedContentItems() {
    val ids = internalScreenState.value.selectedItems.toSet()
    if (ids.isEmpty()) {
        internalScreenState.update { it.copy(selectionMode = false, selectedItems = emptyList()) }
        return
    }
    viewModelScope.launch {
        val activeDeleted = activeItemId != null && activeItemId in ids
        ids.forEach { id ->
            contentItemRepository.getById(id)?.let { contentItemRepository.delete(it) }
        }
        if (activeDeleted) {
            contentItemRepository.clearActiveItem()
            activeItemId = null
            internalScreenState.update { it.copy(focusedContentText = TextValueWrapper()) }
            val remaining = contentItemRepository.getItems().firstOrNull() ?: emptyList()
            if (remaining.isNotEmpty()) loadContentItem(remaining.first()) else createNewContentItem()
        }
        internalScreenState.update {
            it.copy(selectionMode = false, selectedItems = emptyList())
        }
    }
}

/**
 * Extension function for handling UploadActions in PostsViewModel
 */
internal fun PostsViewModel.handleUploadAction(action: UploadAction, contentResolver: ContentResolver) {
    when (action) {
        is UploadAction.SelectFiles -> {
            // Accumulate files from multiple selections (avoid duplicates by URI)
            internalScreenState.update {
                val existingUris = it.selectedFileUris.map { uri -> uri.toString() }.toSet()
                val newUris = action.uris.filter { uri -> uri.toString() !in existingUris }
                it.copy(selectedFileUris = it.selectedFileUris + newUris)
            }
        }

        is UploadAction.ClearSelectedFiles -> {
            internalScreenState.update {
                it.copy(selectedFileUris = emptyList())
            }
        }

        is UploadAction.StartUpload -> {
            val uris = internalScreenState.value.selectedFileUris
            if (uris.isEmpty()) return

            internalScreenState.update { it.copy(isUploading = true) }

            viewModelScope.launch {
                try {
                    // Read all files first
                    val files = uris.mapNotNull { uri ->
                        readFileFromUri(contentResolver, uri)
                    }

                    if (files.isNotEmpty()) {
                        // Upload each file in parallel
                        val uploadJobs = files.map { file ->
                            async {
                                Log.d("PostsViewModel", "Starting upload for: ${file.fileName}")
                                val result = jobQueueRepository.uploadFiles(listOf(file))
                                Pair(file.fileName, result)
                            }
                        }

                        // Wait for all uploads to complete
                        val results = uploadJobs.awaitAll()

                        // Log results
                        var successCount = 0
                        var failCount = 0
                        results.forEach { (fileName, result) ->
                            when (result) {
                                is Result.Success -> {
                                    Log.d("PostsViewModel", "Upload successful for $fileName: ${result.data}")
                                    successCount++
                                }
                                is Result.Error -> {
                                    Log.e("PostsViewModel", "Upload failed for $fileName: ${result.error.debugText}")
                                    failCount++
                                }
                            }
                        }

                        Log.d("PostsViewModel", "Upload complete: $successCount succeeded, $failCount failed")

                        internalScreenState.update {
                            it.copy(
                                selectedFileUris = emptyList(),
                                isUploading = false
                            )
                        }
                    } else {
                        Log.e("PostsViewModel", "No files could be read")
                        internalScreenState.update { it.copy(isUploading = false) }
                    }
                } catch (e: Exception) {
                    Log.e("PostsViewModel", "Upload error", e)
                    internalScreenState.update { it.copy(isUploading = false) }
                }
            }
        }
    }
}

/**
 * Helper function to read file data from a content URI
 */
private fun readFileFromUri(contentResolver: ContentResolver, uri: Uri): FileUploadData? {
    return try {
        val fileName = getFileName(contentResolver, uri) ?: "unknown_file"
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null

        FileUploadData(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes
        )
    } catch (e: Exception) {
        Log.e("PostsViewModel", "Error reading file: ${uri}", e)
        null
    }
}

/**
 * Helper function to get the file name from a content URI
 */
private fun getFileName(contentResolver: ContentResolver, uri: Uri): String? {
    var fileName: String? = null
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                fileName = cursor.getString(nameIndex)
            }
        }
    }
    return fileName
}

// Helper extension functions for keyword confirmation logic
private fun PostsViewModel.confirmHandleSelection() {
    val selectedAccounts = internalScreenState.value.handleList.filter {
        internalScreenState.value.selectedHandles.contains(it.id)
    }
    // Increment usage count for selected handles
    selectedAccounts.forEach { account ->
        viewModelScope.launch {
            keywordRepository.incrementUsage(account.id)
        }
    }

    // Check source to determine behavior
    if (internalScreenState.value.dialogSource == DialogSource.EDITOR) {
        // Insert selected handles into the content editor text
        var updatedText = internalScreenState.value.focusedContentText
        selectedAccounts.forEach { account ->
            updatedText = updatedText.insertText(account.text)
        }

        internalScreenState.update {
            it.copy(
                focusedContentText = updatedText,
                showHandleSelectionDialog = false,
                selectedHandles = emptySet()
            )
        }

        // Auto-save to database
        autoSaveContentItem(updatedText)
    } else {
        // Add selected handles as filter chips
        val newHandleChips = (internalScreenState.value.selectedHandleChips + selectedAccounts).distinctBy { it.id }

        // Apply the keyword filter
        contentItemManager.filterByKeywords(
            handles = newHandleChips.map { it.text },
            tags = internalScreenState.value.selectedTagChips.map { it.text }
        )

        // Rebuild filter list with new chips
        val updatedFilters = internalScreenState.value.filterStateList.buildDateFilterChips(
            internalScreenState.value.startTime,
            internalScreenState.value.endTime,
            newHandleChips,
            internalScreenState.value.selectedTagChips
        )

        internalScreenState.update {
            it.copy(
                selectedHandleChips = newHandleChips,
                filterStateList = updatedFilters,
                showHandleSelectionDialog = false,
                selectedHandles = emptySet()
            )
        }
    }
}

private fun PostsViewModel.confirmTagSelection() {
    val selectedTagItems = internalScreenState.value.tagList.filter {
        internalScreenState.value.selectedTags.contains(it.id)
    }
    // Increment usage count for selected tags
    selectedTagItems.forEach { tag ->
        viewModelScope.launch {
            keywordRepository.incrementUsage(tag.id)
        }
    }

    // Check source to determine behavior
    if (internalScreenState.value.dialogSource == DialogSource.EDITOR) {
        // Insert selected tags into the content editor text
        var updatedText = internalScreenState.value.focusedContentText
        selectedTagItems.forEach { tag ->
            updatedText = updatedText.insertText(tag.text)
        }

        internalScreenState.update {
            it.copy(
                focusedContentText = updatedText,
                showTagSelectionDialog = false,
                selectedTags = emptySet()
            )
        }

        // Auto-save to database
        autoSaveContentItem(updatedText)
    } else {
        // Add selected tags as filter chips
        val newTagChips = (internalScreenState.value.selectedTagChips + selectedTagItems).distinctBy { it.id }

        // Apply the keyword filter
        contentItemManager.filterByKeywords(
            handles = internalScreenState.value.selectedHandleChips.map { it.text },
            tags = newTagChips.map { it.text }
        )

        // Rebuild filter list with new chips
        val updatedFilters = internalScreenState.value.filterStateList.buildDateFilterChips(
            internalScreenState.value.startTime,
            internalScreenState.value.endTime,
            internalScreenState.value.selectedHandleChips,
            newTagChips
        )

        internalScreenState.update {
            it.copy(
                selectedTagChips = newTagChips,
                filterStateList = updatedFilters,
                showTagSelectionDialog = false,
                selectedTags = emptySet()
            )
        }
    }
}

private fun PostsViewModel.confirmKeyWordSelection() {
    val selectedKeyWordItems = internalScreenState.value.keyWordsList.filter {
        internalScreenState.value.selectedKeyWords.contains(it.id)
    }

    // Increment usage count for selected keywords
    selectedKeyWordItems.forEach { keyWord ->
        viewModelScope.launch {
            keywordRepository.incrementUsage(keyWord.id)
        }
    }

    // Check source to determine behavior
    if (internalScreenState.value.dialogSource == DialogSource.EDITOR) {
        // Insert selected keywords into the content editor text
        var updatedText = internalScreenState.value.focusedContentText
        selectedKeyWordItems.forEach { keyWord ->
            updatedText = updatedText.insertText(keyWord.text)
        }

        internalScreenState.update {
            it.copy(
                focusedContentText = updatedText,
                showKeyWordsSelectionDialog = false,
                selectedKeyWords = emptySet()
            )
        }

        // Auto-save to database
        autoSaveContentItem(updatedText)
    } else {
        // For keywords from filter section, we still insert into text
        // (Keywords don't have a filter chip representation)
        var updatedText = internalScreenState.value.focusedContentText
        selectedKeyWordItems.forEach { keyWord ->
            updatedText = updatedText.insertText(keyWord.text)
        }

        internalScreenState.update {
            it.copy(
                focusedContentText = updatedText,
                showKeyWordsSelectionDialog = false,
                selectedKeyWords = emptySet()
            )
        }

        // Auto-save to database
        autoSaveContentItem(updatedText)
    }
}

/**
 * Handles clicking on filter chips.
 * Manages different chip types: TripleState (favorites), DateState, and SingleActionState (@ # and others).
 */
internal fun PostsViewModel.handleChipClick(index: Int) {
    val currentFilters = internalScreenState.value.filterStateList
    if (index !in currentFilters.indices) return

    when (val clickedChip = currentFilters[index]) {
        is FilterState.TripleState -> {
            val updatedFilters = currentFilters.toMutableList()
            val newState = clickedChip.activeState.click()
            updatedFilters[index] = clickedChip.copy(activeState = newState)
            // Apply filter to content manager
            contentItemManager.filterByFavorite(newState.toBoolean())
            internalScreenState.update {
                it.copy(filterStateList = updatedFilters)
            }
        }
        is FilterState.DateState -> {
            // Open bottom sheet to edit date, preselect the appropriate tab
            internalScreenState.update {
                it.copy(
                    showDateFilterSheet = true,
                    selectedDateType = clickedChip.dateType
                )
            }
        }
        is FilterState.SingleActionState -> {
            when {
                // Handle @ chip - open handle selection dialog
                clickedChip.defaultLabel.trim() == "@" -> {
                    consumeAction(UiAction.ShowChips(ChipsType.Handle, cut.the.crap.ui.components.api.Screen.Posts))
                }
                // Handle # chip - open tag selection dialog
                clickedChip.defaultLabel.trim() == "#" -> {
                    consumeAction(UiAction.ShowChips(ChipsType.Tag, cut.the.crap.ui.components.api.Screen.Posts))
                }
                // Check if this is a date-related action
                clickedChip.defaultLabel.contains("Date", ignoreCase = true) ||
                clickedChip.defaultLabel.contains("start", ignoreCase = true) ||
                clickedChip.defaultLabel.contains("end", ignoreCase = true) -> {
                    // Determine which tab to show based on the label
                    val dateType = when {
                        clickedChip.defaultLabel.contains("end", ignoreCase = true) -> DateType.END
                        clickedChip.defaultLabel.contains("start", ignoreCase = true) -> DateType.START
                        else -> DateType.START  // Default to START for generic "Date Range"
                    }
                    // Open bottom sheet for date selection
                    internalScreenState.update {
                        it.copy(
                            showDateFilterSheet = true,
                            selectedDateType = dateType
                        )
                    }
                }
                // For other SingleActionState chips - check if it's a handle/tag chip to remove
                else -> {
                    // Check if this chip is in selectedHandleChips
                    // Note: handle chips display without @ prefix, so we need to compare without it
                    val handleChipToRemove = internalScreenState.value.selectedHandleChips.find {
                        it.text.removePrefix("@") == clickedChip.defaultLabel
                    }

                    if (handleChipToRemove != null) {
                        // Remove this handle chip
                        val newHandleChips = internalScreenState.value.selectedHandleChips.filter {
                            it.id != handleChipToRemove.id
                        }

                        // Update the keyword filter
                        contentItemManager.filterByKeywords(
                            handles = newHandleChips.map { it.text },
                            tags = internalScreenState.value.selectedTagChips.map { it.text }
                        )

                        val updatedFilters = currentFilters.buildDateFilterChips(
                            internalScreenState.value.startTime,
                            internalScreenState.value.endTime,
                            newHandleChips,
                            internalScreenState.value.selectedTagChips
                        )
                        internalScreenState.update {
                            it.copy(
                                selectedHandleChips = newHandleChips,
                                filterStateList = updatedFilters
                            )
                        }
                        return
                    }

                    // Check if this chip is in selectedTagChips
                    val tagChipToRemove = internalScreenState.value.selectedTagChips.find {
                        it.text == clickedChip.defaultLabel
                    }

                    if (tagChipToRemove != null) {
                        // Remove this tag chip
                        val newTagChips = internalScreenState.value.selectedTagChips.filter {
                            it.id != tagChipToRemove.id
                        }

                        // Update the keyword filter
                        contentItemManager.filterByKeywords(
                            handles = internalScreenState.value.selectedHandleChips.map { it.text },
                            tags = newTagChips.map { it.text }
                        )

                        val updatedFilters = currentFilters.buildDateFilterChips(
                            internalScreenState.value.startTime,
                            internalScreenState.value.endTime,
                            internalScreenState.value.selectedHandleChips,
                            newTagChips
                        )
                        internalScreenState.update {
                            it.copy(
                                selectedTagChips = newTagChips,
                                filterStateList = updatedFilters
                            )
                        }
                        return
                    }

                    // For other chips (not handle/tag chips), just toggle chosen state
                    val updatedFilters = currentFilters.toMutableList()
                    updatedFilters[index] = clickedChip.copy(chosen = !clickedChip.chosen)
                    internalScreenState.update {
                        it.copy(filterStateList = updatedFilters)
                    }
                }
            }
        }
    }
}
