package cut.the.crap.ui.content.links

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import cut.the.crap.R
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.tools.LinkMetadata
import cut.the.crap.data.storage.provideOutputStream
//import cut.the.crap.mockedLinkItems
import cut.the.crap.ui.components.BottomNavigationBar
import cut.the.crap.ui.components.DateFilterBottomSheet
import cut.the.crap.ui.components.MenuItem
import cut.the.crap.ui.components.MyEditDialog
import cut.the.crap.ui.components.MyEditDialogStyle
import cut.the.crap.ui.components.MySpeedDialFab
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.components.api.ContentLinkAction
import cut.the.crap.ui.components.api.FileAction
import cut.the.crap.ui.components.api.ListAction
import cut.the.crap.ui.components.api.UiAction
import cut.the.crap.ui.content.posts.DraggableItem
import cut.the.crap.ui.theme.PreviewAppThemeProvider
import cut.the.crap.ui.theme.PreviewThemeWrapper
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkScreen(
    action: (Action) -> Unit,
    itemList: StateFlow<List<ContentLink>>,
    totalCount: StateFlow<Int>,
    screenState: StateFlow<LinksScreenState>,
    navController: NavHostController,
    snackBarMessages: SharedFlow<String>,
) {
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }
    val stateList = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Extract screen state early for use in action handler
    val currentScreenState = screenState.collectAsState().value
    val selectedItems = currentScreenState.selectedItems

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) {
        uri -> uri?.let {
            action(FileAction.Import(uri))
        }
    }

    // Collect snackBar messages
    LaunchedEffect(Unit) {
        snackBarMessages.collect { message ->
            snackBarHostState.showSnackbar(
                message = message,
                duration = androidx.compose.material3.SnackbarDuration.Long
            )
        }
    }

    // Wrapper action handler that intercepts File and List actions
    val actionHandler: (Action) -> Unit = { actionPayload ->
        when (actionPayload) {
            is FileAction.Export -> {
                // Generate filename with timestamp
                val timestamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
                    .format(Date())
                val filename = "links_export_$timestamp.txt"

                // Create outputStream
                val outputStream = provideOutputStream(filename, context)

                // Call action with the outputStream
                action(FileAction.Export(outputStream))
            }

            is FileAction.Import -> {
                // Trigger file picker
                filePickerLauncher.launch("text/plain")
            }

            is ListAction.ScrollToTop -> {
                // Smooth scroll to top with animation
                coroutineScope.launch {
                    stateList.animateScrollToItem(
                        index = 0,
                        scrollOffset = 0
                    )
                }
            }

            is ListAction.SelectAll -> {
                // Always add visible items to selection (merging with existing)
                action(ListAction.SelectAll)
            }

            else -> action(actionPayload)
        }
    }

    val showEditDialog = currentScreenState.showEditDialog
    val checkMarks = currentScreenState.checkMarks
    val showDateFilterSheet = currentScreenState.showDateFilterSheet

    if (showEditDialog != null) {
        MyEditDialog(
            onDismissRequest = { actionHandler(UiAction.ShowEditDialog(null)) },
            onSave = {
                // Execute the action from the dialog (e.g., ContentLinkAction.Delete)
                if (showEditDialog is MyEditDialogStyle.OfferDelete) {
                    actionHandler(showEditDialog.actionPayload)
                }
            },
            style = showEditDialog,
            action = actionHandler
        )
    }

    if (showDateFilterSheet) {
        DateFilterBottomSheet(
            initialStartTime = currentScreenState.startTime,
            initialEndTime = currentScreenState.endTime,
            initialDateType = currentScreenState.selectedDateType,
            onDismiss = { actionHandler(UiAction.ShowDateFilterSheet(false, cut.the.crap.ui.components.api.Screen.Links)) },
            onConfirm = { startTime, endTime ->
                actionHandler(UiAction.SetDateFilter(startTime, endTime, cut.the.crap.ui.components.api.Screen.Links))
                actionHandler(UiAction.ShowDateFilterSheet(false, cut.the.crap.ui.components.api.Screen.Links))
            }
        )
    }

    // Show Comment/Quote Dialog
    val showCommentQuoteDialog = currentScreenState.showCommentQuoteDialog
    if (showCommentQuoteDialog != null) {
        CommentQuoteDialog(
            contentLink = showCommentQuoteDialog,
            onDismiss = {
                // Close the dialog by setting showCommentQuoteDialog to null
                actionHandler(ContentLinkAction.ShowCommentQuoteDialog(ContentLink()))
            },
            onComment = { link, comment ->
                actionHandler(ContentLinkAction.CreateComment(link, comment))
            },
            onQuote = { link, quote ->
                actionHandler(ContentLinkAction.CreateQuote(link, quote))
            }
        )
    }

    val showHandleDialog = currentScreenState.showHandleSelectionDialog
    val handleList = currentScreenState.handleList
    val selectedHandles = currentScreenState.selectedHandles

    if (showHandleDialog) {
        cut.the.crap.ui.components.KeywordSelectionDialog(
            type = cut.the.crap.ui.components.api.ChipsType.Handle,
            items = handleList,
            selectedItems = selectedHandles,
            onItemToggle = { keyword ->
                actionHandler(cut.the.crap.ui.components.api.KeywordAction.ToggleHandleSelection(keyword.id))
            },
            onConfirm = { actionHandler(cut.the.crap.ui.components.api.KeywordAction.ConfirmHandleSelection) },
            onDismiss = { actionHandler(UiAction.ShowKeywordSelectionDialog(cut.the.crap.ui.components.api.ChipsType.Handle, false, cut.the.crap.ui.components.api.Screen.Links)) },
            onAdd = actionHandler
        )
    }

    // Show Keyword Management Dialog for ContentLink items
    val showKeywordDialog = currentScreenState.showKeywordSelectionDialog
    val currentEditingLink = currentScreenState.currentEditingLink
    val currentKeywordType = currentScreenState.currentKeywordType
    val tagList = currentScreenState.tagList
    val keyWordList = currentScreenState.keyWordList

    if (showKeywordDialog && currentEditingLink != null) {
        val masterList = when (currentKeywordType) {
            cut.the.crap.ui.components.api.ChipsType.Handle -> handleList
            cut.the.crap.ui.components.api.ChipsType.Tag -> tagList
            cut.the.crap.ui.components.api.ChipsType.KeyWords -> keyWordList
            else -> emptyList()
        }

        // Track the selected tag *texts* for this link, seeded from its current
        // description. Text-based (not id-based) so a freshly added keyword selects
        // immediately, before the master list round-trips through the repository.
        var selectedTexts by remember(currentEditingLink.id, currentKeywordType) {
            mutableStateOf(
                when (currentKeywordType) {
                    cut.the.crap.ui.components.api.ChipsType.Handle ->
                        LinkMetadata.getHandles(currentEditingLink)
                    cut.the.crap.ui.components.api.ChipsType.Tag ->
                        LinkMetadata.getHashtags(currentEditingLink)
                    cut.the.crap.ui.components.api.ChipsType.KeyWords ->
                        LinkMetadata.getKeywords(currentEditingLink)
                    else -> emptyList()
                }.toSet()
            )
        }

        cut.the.crap.ui.components.KeywordSelectionDialog(
            type = currentKeywordType,
            items = masterList,
            selectedItems = masterList.filter { it.text in selectedTexts }.map { it.id }.toSet(),
            onItemToggle = { keyword ->
                selectedTexts = if (keyword.text in selectedTexts) {
                    selectedTexts - keyword.text
                } else {
                    selectedTexts + keyword.text
                }
            },
            onConfirm = {
                // Write the chosen tags back onto the link, reusing the same persistence
                // action the inline remove-chips use.
                val updated = LinkMetadata.setTags(
                    currentEditingLink, selectedTexts.toList(), currentKeywordType
                )
                actionHandler(ContentLinkAction.EditSearchHint(currentEditingLink, updated.description))
                // Close dialog (id == -1 sentinel)
                actionHandler(ContentLinkAction.ManageKeywords(ContentLink(), currentKeywordType))
            },
            onDismiss = {
                actionHandler(ContentLinkAction.ManageKeywords(ContentLink(), currentKeywordType))
            },
            onAdd = { addAction ->
                // Forward to the ViewModel (persists new hashtags/keywords to the master
                // list; deletes pass through too) and, for new entries, select them here.
                actionHandler(addAction)
                val addedText = when (addAction) {
                    is cut.the.crap.ui.components.api.KeywordAction.AddHandle -> addAction.text
                    is cut.the.crap.ui.components.api.KeywordAction.AddTag -> addAction.text
                    is cut.the.crap.ui.components.api.KeywordAction.AddKeyWord -> addAction.text
                    else -> null
                }
                addedText?.let { selectedTexts = selectedTexts + it }
            }
        )
    }

    // Bulk-tag dialog: applies one tag category to the whole current selection at once. Reuses the
    // single-link keyword picker, but starts with an empty selection since bulk tagging is additive
    // across links that each already carry different tags.
    val bulkTagType = currentScreenState.bulkTagType
    if (bulkTagType != null) {
        val bulkMasterList = when (bulkTagType) {
            cut.the.crap.ui.components.api.ChipsType.Handle -> handleList
            cut.the.crap.ui.components.api.ChipsType.Tag -> tagList
            cut.the.crap.ui.components.api.ChipsType.KeyWords -> keyWordList
            else -> emptyList()
        }
        var bulkSelectedTexts by remember(bulkTagType) { mutableStateOf(emptySet<String>()) }

        cut.the.crap.ui.components.KeywordSelectionDialog(
            type = bulkTagType,
            items = bulkMasterList,
            selectedItems = bulkMasterList.filter { it.text in bulkSelectedTexts }.map { it.id }.toSet(),
            onItemToggle = { keyword ->
                bulkSelectedTexts = if (keyword.text in bulkSelectedTexts) {
                    bulkSelectedTexts - keyword.text
                } else {
                    bulkSelectedTexts + keyword.text
                }
            },
            onConfirm = {
                actionHandler(ListAction.TagSelected(bulkTagType, bulkSelectedTexts.toList()))
                actionHandler(UiAction.ShowBulkTagDialog(null, cut.the.crap.ui.components.api.Screen.Links))
            },
            onDismiss = {
                actionHandler(UiAction.ShowBulkTagDialog(null, cut.the.crap.ui.components.api.Screen.Links))
            },
            onAdd = { addAction ->
                // Persist a brand-new keyword to the master list and select it here immediately.
                actionHandler(addAction)
                val addedText = when (addAction) {
                    is cut.the.crap.ui.components.api.KeywordAction.AddHandle -> addAction.text
                    is cut.the.crap.ui.components.api.KeywordAction.AddTag -> addAction.text
                    is cut.the.crap.ui.components.api.KeywordAction.AddKeyWord -> addAction.text
                    else -> null
                }
                addedText?.let { bulkSelectedTexts = bulkSelectedTexts + it }
            }
        )
    }

    val stateValue by itemList.collectAsState()
    val total by totalCount.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = linksTopBar(
                screenState = currentScreenState,
                currentItemCount = stateValue.size,
                totalItemCount = total,
                visibleItemIds = stateValue.map { it.id },
                action = actionHandler,
            ),
            bottomBar = { BottomNavigationBar(navController = navController) },
            floatingActionButton = {
                MySpeedDialFab(
                    action = actionHandler,
                    actions = listOf(
                        MenuItem(
                            title = R.string.app_name,
                            icon = Icons.Filled.ArrowUpward,
                            actionPayload = ListAction.ScrollToTop
                        ),
                        MenuItem(
                            title = R.string.app_name,
                            icon = Icons.Filled.FolderOpen,
                            actionPayload = FileAction.Import(Uri.EMPTY) // Will be intercepted
                        ),
                        MenuItem(
                            title = R.string.app_name,
                            icon = Icons.Filled.Download,
                            actionPayload = FileAction.Export(null) // Will be intercepted
                        ),
                        MenuItem(
                            title = R.string.app_name,
                            icon = Icons.Filled.SwapVert,
                            actionPayload = ListAction.InvertList
                        ),
                    )
                )
            }
        ) { paddingValues ->
            var draggingItemIndex: Int? by remember {
                mutableStateOf(null)
            }

            var delta: Float by remember {
                mutableFloatStateOf(0f)
            }

            var draggingItem: LazyListItemInfo? by remember {
                mutableStateOf(null)
            }

            val scrollChannel = Channel<Float>()

            LaunchedEffect(stateList) {
                while (true) {
                    val diff = scrollChannel.receive()
                    stateList.scrollBy(diff)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .pointerInput(key1 = stateList) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                stateList.layoutInfo.visibleItemsInfo
                                    .firstOrNull { item ->
                                        offset.y.toInt() in
                                            item.offset..(item.offset + item.size)
                                    }
                                    ?.also {
                                        (it.contentType as? DraggableItem)?.let { draggableItem ->
                                            draggingItem = it
                                            draggingItemIndex = draggableItem.index
                                        }
                                    }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                delta += dragAmount.y

                                val currentDraggingItemIndex =
                                    draggingItemIndex ?: return@detectDragGesturesAfterLongPress
                                val currentDraggingItem =
                                    draggingItem ?: return@detectDragGesturesAfterLongPress

                                val startOffset = currentDraggingItem.offset + delta
                                val endOffset =
                                    currentDraggingItem.offset + currentDraggingItem.size + delta
                                val middleOffset = startOffset + (endOffset - startOffset) / 2

                                val targetItem =
                                    stateList.layoutInfo.visibleItemsInfo.find { item ->
                                        middleOffset.toInt() in item.offset..item.offset + item.size &&
                                            currentDraggingItem.index != item.index &&
                                            item.contentType is DraggableItem
                                    }

                                if (targetItem != null) {
                                    val targetIndex = (targetItem.contentType as DraggableItem).index
//                                onMove(currentDraggingItemIndex, targetIndex)
                                    draggingItemIndex = targetIndex
                                    delta += currentDraggingItem.offset - targetItem.offset
                                    draggingItem = targetItem
                                } else {
                                    val startOffsetToTop =
                                        startOffset - stateList.layoutInfo.viewportStartOffset
                                    val endOffsetToBottom =
                                        endOffset - stateList.layoutInfo.viewportEndOffset
                                    val scroll =
                                        when {
                                            startOffsetToTop < 0 -> startOffsetToTop.coerceAtMost(0f)
                                            endOffsetToBottom > 0 -> endOffsetToBottom.coerceAtLeast(0f)
                                            else -> 0f
                                        }
                                    val canScrollDown =
                                        currentDraggingItemIndex != stateValue.size - 1 && endOffsetToBottom > 0
                                    val canScrollUp = currentDraggingItemIndex != 0 && startOffsetToTop < 0
                                    if (scroll != 0f && (canScrollUp || canScrollDown)) {
                                        scrollChannel.trySend(scroll)
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingItem = null
                                draggingItemIndex = null
                                delta = 0f
                            },
                            onDragCancel = {
                                draggingItem = null
                                draggingItemIndex = null
                                delta = 0f
                            },
                        )
                    },
                state = stateList,
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                //stickyHeader {}

                itemsIndexed(
                    items = stateValue,
                    key = { _, item -> item.id },
                    contentType = { index, _ -> DraggableItem(index = index) }) { index, item ->
                    val modifier = if (draggingItemIndex == index) {
                        Modifier
                            .zIndex(1f)
                            .graphicsLayer {
                                translationY = delta
                            }
                    } else {
                        Modifier
                    }
                    LinkListItem(
                        modifier = modifier,
                        item = item,
                        isChecked = selectedItems.contains(item.id),//(draggingItemIndex != null && draggingItemIndex == index),
                        action = actionHandler,
                        selectionState = checkMarks
                    )
                }

                // Spacer at the end to allow scrolling above FAB
                item {
                    Spacer(
                        modifier = Modifier.padding(bottom = 80.dp)
                    )
                }
            }
        }

        // SnackBar positioned at bottom TODO there is a better fix using correct insets
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) {
            SnackbarHost(hostState = snackBarHostState)
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
private fun Preview(
    @PreviewParameter(PreviewAppThemeProvider::class) theme: PreviewThemeWrapper,
) {
    theme {
//        val itemListFlow = MutableStateFlow(emptyList())  //mockedLinkItems
        val totalCountFlow = MutableStateFlow(3)
        val screenStateFlow = MutableStateFlow(LinksScreenState())
        val snackBarFlow = MutableSharedFlow<String>()

        LinkScreen(
            action = {},
            itemList = MutableStateFlow(emptyList()),  // itemListFlow
            totalCount = totalCountFlow,
            screenState = screenStateFlow,
            navController = rememberNavController(),
            snackBarMessages = snackBarFlow
        )
    }
}