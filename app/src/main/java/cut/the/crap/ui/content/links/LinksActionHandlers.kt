package cut.the.crap.ui.content.links

import androidx.lifecycle.viewModelScope
import cut.the.crap.data.domain.KeyWord
import cut.the.crap.data.domain.KeywordType
import cut.the.crap.ui.components.DateType
import cut.the.crap.ui.components.FilterState
import cut.the.crap.ui.components.MyEditDialogStyle
import cut.the.crap.data.rest.task.ShareLinksTask
import cut.the.crap.ui.components.api.*
import cut.the.crap.tools.ensureTrailingSpace
import cut.the.crap.tools.isForThisScreen
import cut.the.crap.tools.normalizeToStartOfDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Extension functions for handling ContentLinkActions in LinksViewModel
 */

internal fun LinksViewModel.handleContentLinkAction(action: ContentLinkAction) {
    when (action) {
        is ContentLinkAction.Delete -> viewModelScope.launch {
            contentRepository.delete(action.item)
        }

        is ContentLinkAction.OfferDelete -> internalScreenState.update {
            it.copy(
                showEditDialog = MyEditDialogStyle.OfferDelete(
                    title = "Delete Link",
                    dismiss = "Cancel",
                    actionPayload = ContentLinkAction.Delete(action.item)
                )
            )
        }

        is ContentLinkAction.EditSearchHint -> {
            updateContentLink(action.item.copy(description = action.searchHint))
        }

        is ContentLinkAction.ToggleFavourite -> {
            updateContentLink(action.item.copy(favourite = !action.item.favourite))
        }

        is ContentLinkAction.ToggleSelection -> {
            val list = screenState.value.selectedItems.toMutableList()
            val resultList = if (list.contains(action.item.id)) {
                list.filter { it != action.item.id }
            } else {
                list.add(action.item.id)
                list
            }
            internalScreenState.update { it.copy(selectedItems = resultList) }
        }

        is ContentLinkAction.EnterSelectionMode -> {
            // Enter selection mode and select this item
            internalScreenState.update {
                it.copy(
                    checkMarks = true,
                    selectedItems = listOf(action.item.id)
                )
            }
        }

        is ContentLinkAction.ShowCommentQuoteDialog -> {
            // If item.id is -1 (default/empty ContentLink), close the dialog, otherwise show it
            internalScreenState.update {
                it.copy(showCommentQuoteDialog = if (action.item.id == -1) null else action.item)
            }
        }

        is ContentLinkAction.CreateComment -> {
            // Comments are posted as regular tweets with the comment text
            // The MainActivity will handle launching the Twitter intent
            internalScreenState.update { it.copy(showCommentQuoteDialog = null) }
        }

        is ContentLinkAction.CreateQuote -> {
            // Quote tweets use the TwitterIntent.QuoteTweet functionality
            // The MainActivity will handle extracting the tweet ID and launching the intent
            internalScreenState.update { it.copy(showCommentQuoteDialog = null) }
        }

        is ContentLinkAction.ManageKeywords -> {
            // If item id is -1, close the dialog, otherwise open it
            if (action.item.id == -1) {
                internalScreenState.update {
                    it.copy(
                        showKeywordSelectionDialog = false,
                        currentEditingLink = null
                    )
                }
            } else {
                internalScreenState.update {
                    it.copy(
                        showKeywordSelectionDialog = true,
                        currentEditingLink = action.item,
                        currentKeywordType = action.type
                    )
                }
            }
        }

        // Actions not handled by this ViewModel (handled at Activity level)
        else -> Unit
    }
}

/**
 * Extension functions for handling TextActions in LinksViewModel
 */
internal fun LinksViewModel.handleTextAction(action: TextAction) {
    when (action) {
        is TextAction.PostQuery -> {
            internalScreenState.update { it.copy(query = action.query) }
            // Apply visible query to database filter
            itemManager.filterByLinkSubstring(action.query)
        }

        is TextAction.AddHiddenFilter -> {
            val newFilters = (screenState.value.hiddenFilters + action.keyword).distinct()
            internalScreenState.update { it.copy(hiddenFilters = newFilters) }
        }

        is TextAction.RemoveHiddenFilter -> {
            val newFilters = screenState.value.hiddenFilters.filter { it != action.keyword }
            internalScreenState.update { it.copy(hiddenFilters = newFilters) }
        }

        is TextAction.ClearHiddenFilters -> {
            internalScreenState.update { it.copy(hiddenFilters = emptyList()) }
        }

        is TextAction.SetStartDateFilter -> {
            if (!this.isForThisScreen(action.screen)) return

            // Normalize the timestamp to midnight (00:00:00) of that day
            val normalizedTimestamp = normalizeToStartOfDay(action.timestamp)

            // Update the date filter in ItemManager
            itemManager.updateFilterState(
                mapOf("timeFrameStart" to normalizedTimestamp)
            )

            internalScreenState.update {
                // Rebuild filter chips to properly reflect the new start date
                val updatedFilters = it.filterStateList.buildDateFilterChips(
                    startTime = normalizedTimestamp,
                    endTime = it.endTime
                )

                it.copy(
                    filterStateList = updatedFilters,
                    startTime = normalizedTimestamp
                )
            }
        }

        is TextAction.InsertText -> internalScreenState.update {
            it.copy(editText = it.editText.ensureTrailingSpace() + action.text, textInputExpanded = true)
        }

        else -> Unit
    }
}

/**
 * Extension functions for handling UiActions in LinksViewModel
 */
internal fun LinksViewModel.handleUiAction(action: UiAction) {
    when (action) {
        is UiAction.ChipClicked -> {
            if (!this.isForThisScreen(action.screen)) return
            when (val clickedChip = screenState.value.filterStateList[action.index]) {
                is FilterState.TripleState -> {
                    val newList = screenState.value.filterStateList.click(action.index)
                    itemManager.filterByFavourite((newList[action.index] as FilterState.TripleState).activeState.toBoolean())
                    internalScreenState.update { it.copy(filterStateList = newList) }
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
                    // Check if this is a date-related action
                    if (clickedChip.defaultLabel.contains("Date", ignoreCase = true) ||
                        clickedChip.defaultLabel.contains("start", ignoreCase = true) ||
                        clickedChip.defaultLabel.contains("end", ignoreCase = true)
                    ) {
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
                    } else {
                        // For other SingleActionState chips, toggle chosen state
                        val newList = screenState.value.filterStateList.click(action.index)
                        internalScreenState.update { it.copy(filterStateList = newList) }
                    }
                }
            }
        }

        is UiAction.ShowDateFilterSheet -> {
            if (!this.isForThisScreen(action.screen)) return
            internalScreenState.update {
                it.copy(showDateFilterSheet = action.show)
            }
        }

        is UiAction.SetDateFilter -> {
            if (!this.isForThisScreen(action.screen)) return
            itemManager.updateFilterState(
                mapOf(
                    "timeFrameStart" to action.startTime,
                    "timeFrameEnd" to action.endTime
                )
            )

            internalScreenState.update {
                // Update filter state list to show appropriate date chips
                val updatedFilters = it.filterStateList.buildDateFilterChips(action.startTime, action.endTime)

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

                itemManager.updateFilterState(
                    mapOf(
                        "timeFrameStart" to newStart,
                        "timeFrameEnd" to newEnd
                    )
                )

                // Update filter state list to show appropriate date chips
                val updatedFilters = it.filterStateList.buildDateFilterChips(newStart, newEnd)

                it.copy(
                    startTime = newStart,
                    endTime = newEnd,
                    filterStateList = updatedFilters
                )
            }
        }

        is UiAction.ShowEditDialog -> {
            if (!this.isForThisScreen(action.screen)) return
            internalScreenState.update {
                it.copy(showEditDialog = action.dialog)
            }
        }

        is UiAction.ExpandSearch -> {
            if (!this.isForThisScreen(action.screen)) return
            internalScreenState.update {
                it.copy(searchExpanded = action.expanded)
            }
        }

        is UiAction.ExpandTextInput -> {
            if (!this.isForThisScreen(action.screen)) return
            internalScreenState.update {
                it.copy(textInputExpanded = action.expanded)
            }
        }

        is UiAction.ShowKeywordSelectionDialog -> {
            if (!this.isForThisScreen(action.screen)) return
            internalScreenState.update {
                it.copy(showHandleSelectionDialog = action.show)
            }
        }

        is UiAction.ShowChips -> {
            if (!this.isForThisScreen(action.screen)) return
            when (action.type) {
                ChipsType.Handle -> internalScreenState.update {
                    it.copy(showHandleSelectionDialog = !it.showHandleSelectionDialog)
                }

                else -> {} // Other types not used on Links screen
            }
        }

        is UiAction.ExitSelectionMode -> {
            if (!this.isForThisScreen(action.screen)) return
            // Exit selection mode and clear selections
            internalScreenState.update {
                it.copy(
                    checkMarks = false,
                    selectedItems = emptyList()
                )
            }
        }
    }
}

/**
 * Extension functions for handling KeywordActions in LinksViewModel
 */
internal fun LinksViewModel.handleKeywordAction(action: KeywordAction) {
    when (action) {
        is KeywordAction.AddHandle -> {
            // Handles are derived from existing links' usernames, so there's no master
            // list to persist to. The new handle is still written onto the edited link
            // by the dialog's onConfirm (see LinksScreen).
        }

        is KeywordAction.AddTag -> {
            // Persist a new hashtag to the master list so it's reusable across links.
            viewModelScope.launch {
                keywordRepository.insert(KeyWord(text = action.text, type = KeywordType.HASHTAG))
            }
        }

        is KeywordAction.AddKeyWord -> {
            // Persist a new keyword to the master list so it's reusable across links.
            viewModelScope.launch {
                keywordRepository.insert(KeyWord(text = action.text, type = KeywordType.TAG))
            }
        }

        is KeywordAction.ToggleHandleSelection -> {
            internalScreenState.update {
                val currentSelection = it.selectedHandles.toMutableSet()
                if (currentSelection.contains(action.handleId)) {
                    currentSelection.remove(action.handleId)
                } else {
                    currentSelection.add(action.handleId)
                }
                it.copy(selectedHandles = currentSelection)
            }
        }

        is KeywordAction.ConfirmHandleSelection -> {
            val selectedHandleTags = screenState.value.handleList.filter {
                screenState.value.selectedHandles.contains(it.id)
            }
            // Add all selected handles as hidden filters
            val newFilters = (screenState.value.hiddenFilters + selectedHandleTags.map { it.text }).distinct()

            internalScreenState.update {
                it.copy(
                    hiddenFilters = newFilters,
                    showHandleSelectionDialog = false,
                    selectedHandles = emptySet()
                )
            }
        }

        is KeywordAction.DeleteHandle -> {
            // Not needed - usernames are extracted from visible links
        }

        is KeywordAction.DeleteHandlesBulk -> {
            // Not needed - usernames are extracted from visible links
        }

        is KeywordAction.ToggleFavorite -> {
            // Not needed - usernames are extracted from visible links
        }

        // Other actions not used on Links screen
        else -> Unit
    }
}

/**
 * Extension functions for handling ListActions in LinksViewModel
 */
internal fun LinksViewModel.handleListAction(action: ListAction) {
    when (action) {
        ListAction.InvertList -> itemManager.reverseOrder()

        ListAction.SelectAll -> {
            // Add all currently visible items to the selection (keep existing selections)
            val visibleItemIds = listState.value.map { it.id }
            val mergedSelection = (screenState.value.selectedItems + visibleItemIds).distinct()
            internalScreenState.update { it.copy(selectedItems = mergedSelection) }
        }

        ListAction.DeselectAll -> {
            // Clear all selections
            internalScreenState.update { it.copy(selectedItems = emptyList()) }
        }

        ListAction.DeleteAll -> {
            viewModelScope.launch {
                listState.value.forEach {
                    contentRepository.delete(it)
                }
            }
        }

        ListAction.ToggleFavoritesForSelected -> {
            viewModelScope.launch {
                // Get all selected items from the current list
//                val selectedItems = listState.value.filter { it.id in screenState.value.selectedItems }
//
//                // Check if all selected items are already favorited
//                val allFavorited = selectedItems.all { it.favourite }
//
//                // Toggle: if all are favorited, unfavorite all; otherwise, favorite all
//                val newFavoriteState = !allFavorited
//
//                selectedItems.forEach { item ->
//                    updateContentLink(item.copy(favourite = newFavoriteState))
//                }
            }
        }

        ListAction.FireJob -> {
            viewModelScope.launch {
                val selectedIds = screenState.value.selectedItems
                val selectedLinks = listState.value
                    .filter { it.id in selectedIds }
                    .map { it.link }

                if (selectedLinks.isNotEmpty()) {
                    when (val result = jobQueueRepository.submitTask(ShareLinksTask(selectedLinks))) {
                        is cut.the.crap.data.rest.Result.Success -> {
                            emitSnackBarMessage("Submitted ${selectedLinks.size} links to job queue")
                        }
                        is cut.the.crap.data.rest.Result.Error -> {
                            emitSnackBarMessage("Failed to submit: ${result.message}")
                        }
                    }
                }
            }
        }

        else -> Unit
    }
}

/**
 * Extension functions for handling FileActions in LinksViewModel
 */
internal fun LinksViewModel.handleFileAction(action: FileAction) {
    when (action) {
        is FileAction.Export -> {
            viewModelScope.launch(Dispatchers.IO) {
                val result = exportSelectedItems(action.outputStream)
                result.onSuccess { message ->
                    emitSnackBarMessage(message)
                }.onFailure { error ->
                    emitSnackBarMessage("Export failed: ${error.message}")
                }
            }
        }

        is FileAction.Import -> {
            viewModelScope.launch(Dispatchers.IO) {
                val result = importFromFile(action.uri, context)
                result.onSuccess { message ->
                    emitSnackBarMessage(message)
                }.onFailure { error ->
                    emitSnackBarMessage("Import failed: ${error.message}")
                }
            }
        }

        is FileAction.BackupDatabase -> {
            // Handled in MainActivity
        }

        is FileAction.RestoreDatabase -> {
            // Handled in MainActivity
        }
    }
}
