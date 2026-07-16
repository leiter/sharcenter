package cut.the.crap.ui.content.posts

import cut.the.crap.platform.rememberFilePicker
import cut.the.crap.platform.toPlatformUri

import cut.the.crap.platform.NotificationDuration

import cut.the.crap.platform.Notifier

import org.koin.compose.koinInject

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.stringResource
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.dialog_delete
import cut.the.crap.shared.resources.links_bulk_delete
import cut.the.crap.shared.resources.links_cd_exit_selection
import cut.the.crap.shared.resources.links_deselect_all
import cut.the.crap.shared.resources.links_select_all_filtered
import cut.the.crap.shared.resources.posts_cd_collapse_filters
import cut.the.crap.shared.resources.posts_cd_create_item
import cut.the.crap.shared.resources.posts_cd_expand_filters
import cut.the.crap.shared.resources.posts_cd_load_stats
import cut.the.crap.shared.resources.posts_selection_count
import cut.the.crap.shared.resources.posts_snackbar_cleared
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import cut.the.crap.data.domain.ContentItem
import cut.the.crap.ui.components.ActiveState
import cut.the.crap.ui.components.BottomNavigationBar
import cut.the.crap.ui.components.FilterState
import cut.the.crap.ui.components.KeywordSelectionDialog
import cut.the.crap.ui.components.MyChip
import cut.the.crap.ui.components.MyEditDialog
import cut.the.crap.ui.components.MyIconAction
import cut.the.crap.ui.components.MySearchBar
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.components.api.ChipsType
import cut.the.crap.ui.components.api.KeywordAction
import cut.the.crap.ui.components.api.TextAction
import cut.the.crap.ui.components.api.UiAction
import cut.the.crap.ui.components.api.UploadAction
import cut.the.crap.ui.components.api.ContentItemAction
import cut.the.crap.ui.localizedText
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun PostsScreen(
    action: (Action) -> Unit,
    navController: NavHostController,
    screenState: StateFlow<PostsScreenState>,
    contentItems: StateFlow<List<ContentItem>>,
    onContentItemsReordered: (List<ContentItem>) -> Unit,
    snackBarEvents: SharedFlow<PostsSnackbarEvent> = MutableSharedFlow(),
    eciLoading: StateFlow<Boolean> = MutableStateFlow(false),
    eciEvents: SharedFlow<EciUiEvent> = MutableSharedFlow(),
    onLoadEciStatistics: () -> Unit = {},
    ) {
    val snackbarHostState = remember { SnackbarHostState() }
    val notifier: Notifier = koinInject()
    val context = LocalContext.current
    val isEciLoading by eciLoading.collectAsState()

    // Navigate to the statistics table on success, or toast the error.
    LaunchedEffect(Unit) {
        eciEvents.collect { event ->
            when (event) {
                EciUiEvent.NavigateToTable -> navController.navigate("eci_statistics")
                is EciUiEvent.ShowError ->
                    notifier.show(event.error.localizedText(), NotificationDuration.Long)
            }
        }
    }

    // Snackbar strings resolved here since showSnackbar runs outside composable scope.
    val postClearedMessage = stringResource(Res.string.posts_snackbar_cleared)
    val deleteActionLabel = stringResource(Res.string.dialog_delete)

    // Show a snackbar (with a Delete action) when the editor is cleared,
    // letting the user also delete the post that was detached from the editor.
    LaunchedEffect(Unit) {
        snackBarEvents.collect { event ->
            when (event) {
                is PostsSnackbarEvent.OfferDeleteClearedItem -> {
                    val result = snackbarHostState.showSnackbar(
                        message = postClearedMessage,
                        actionLabel = deleteActionLabel,
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        action(ContentItemAction.Delete(event.itemId))
                    }
                }
            }
        }
    }

    var query by remember {
        mutableStateOf(screenState.value.query)
    }
    var searchExpanded by remember {
        mutableStateOf(screenState.value.searchExpanded)
    }
    var showEditDialog: Boolean by remember {
        mutableStateOf(false)
    }

    val showHandleDialog = screenState.collectAsState().value.showHandleSelectionDialog
    val showTagDialog = screenState.collectAsState().value.showTagSelectionDialog
    val showKeyWordsDialog = screenState.collectAsState().value.showKeyWordsSelectionDialog

    val handleList = screenState.collectAsState().value.handleList
    val tagList = screenState.collectAsState().value.tagList
    val keyWordsList = screenState.collectAsState().value.keyWordsList

    val selectedHandles = screenState.collectAsState().value.selectedHandles
    val selectedTags = screenState.collectAsState().value.selectedTags
    val selectedKeyWords = screenState.collectAsState().value.selectedKeyWords

    // File picker for selecting multiple documents to upload
    val multipleFilesPicker = rememberFilePicker(
        mimeTypes = listOf("*/*"),
        allowMultiple = true,
    ) { uris ->
        if (uris.isNotEmpty()) {
            action(UploadAction.SelectFiles(uris))
        }
    }

    if (showEditDialog) {
        MyEditDialog(
            onDismissRequest = { showEditDialog = false },
            onSave = {},
            action = action
        )
    }

    if (showHandleDialog) {
        KeywordSelectionDialog(
            type = ChipsType.Handle,
            items = handleList,
            selectedItems = selectedHandles,
            onItemToggle = { keyWord ->
                action(KeywordAction.ToggleHandleSelection(keyWord.id))
            },
            onConfirm = { action(KeywordAction.ConfirmHandleSelection) },
            onDismiss = { action(UiAction.ShowKeywordSelectionDialog(ChipsType.Handle, false, cut.the.crap.ui.components.api.Screen.Posts)) },
            onAdd = action
        )
    }

    if (showTagDialog) {
        KeywordSelectionDialog(
            type = ChipsType.Tag,
            items = tagList,
            selectedItems = selectedTags,
            onItemToggle = { tag ->
                action(KeywordAction.ToggleTagSelection(tag.id))
            },
            onConfirm = { action(KeywordAction.ConfirmTagSelection) },
            onDismiss = { action(UiAction.ShowKeywordSelectionDialog(ChipsType.Tag, false, cut.the.crap.ui.components.api.Screen.Posts)) },
            onAdd = action
        )
    }

    if (showKeyWordsDialog) {
        KeywordSelectionDialog(
            type = ChipsType.KeyWords,
            items = keyWordsList,
            selectedItems = selectedKeyWords,
            onItemToggle = { keyWord ->
                action(KeywordAction.ToggleKeyWordSelection(keyWord.id))
            },
            onConfirm = { action(KeywordAction.ConfirmKeyWordSelection) },
            onDismiss = { action(UiAction.ShowKeywordSelectionDialog(ChipsType.KeyWords, false, cut.the.crap.ui.components.api.Screen.Posts)) },
            onAdd = action
        )
    }

    val screenStateValue = screenState.collectAsState().value
    val filterExpanded = screenStateValue.filterExpanded
    val filterStateList = screenStateValue.filterStateList

    // System back exits batch selection before leaving the screen.
    BackHandler(enabled = screenStateValue.selectionMode) {
        action(UiAction.ExitSelectionMode(cut.the.crap.ui.components.api.Screen.Posts))
    }

    if (screenStateValue.showDateFilterSheet) {
        cut.the.crap.ui.components.DateFilterBottomSheet(
            initialStartTime = screenStateValue.startTime,
            initialEndTime = screenStateValue.endTime,
            initialDateType = screenStateValue.selectedDateType,
            onDismiss = { action(UiAction.ShowDateFilterSheet(false, cut.the.crap.ui.components.api.Screen.Posts)) },
            onConfirm = { start, end ->
                action(UiAction.SetDateFilter(start, end, cut.the.crap.ui.components.api.Screen.Posts))
                action(UiAction.ShowDateFilterSheet(false, cut.the.crap.ui.components.api.Screen.Posts))
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (screenStateValue.selectionMode) {
                val allItems by contentItems.collectAsState()
                val selectedCount = screenStateValue.selectedItems.size
                val visibleIds = allItems.map { it.id }
                val allSelected = visibleIds.isNotEmpty() &&
                    visibleIds.all { it in screenStateValue.selectedItems }
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.posts_selection_count, selectedCount),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            action(UiAction.ExitSelectionMode(cut.the.crap.ui.components.api.Screen.Posts))
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(Res.string.links_cd_exit_selection)
                            )
                        }
                    },
                    actions = {
                        // Select-all / deselect-all toggle over the currently visible items.
                        IconButton(onClick = {
                            if (allSelected) action(ContentItemAction.DeselectAll)
                            else action(ContentItemAction.SelectAll)
                        }) {
                            Icon(
                                imageVector = if (allSelected) Icons.Filled.Cancel else Icons.Filled.CheckCircle,
                                tint = if (allSelected) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.secondary,
                                contentDescription = stringResource(
                                    if (allSelected) Res.string.links_deselect_all
                                    else Res.string.links_select_all_filtered
                                )
                            )
                        }
                        if (selectedCount > 0) {
                            IconButton(onClick = { action(ContentItemAction.DeleteSelected) }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(Res.string.links_bulk_delete)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            } else {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("") },
                    actions = {
                            MySearchBar(
                                query = query,
                                expanded = searchExpanded,
                                onQueryChanged = { query = it; action(TextAction.EditQueryText(it)) },
                                onQuerySubmit = { },
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                                onExpandedChanged = {
                                    searchExpanded = it
                                    action(UiAction.ExpandSearch(it, cut.the.crap.ui.components.api.Screen.Posts))
                                }
                            )

                        // Determine if there are active filter chips
                        val hasActiveFilters = filterStateList.any { filter ->
                            when (filter) {
                                is FilterState.TripleState -> filter.activeState != ActiveState.Default
                                is FilterState.DateState -> filter.date != null
                                else -> false
                            }
                        } || screenStateValue.selectedHandleChips.isNotEmpty() || screenStateValue.selectedTagChips.isNotEmpty()

                        MyIconAction(
                            iconPainter = rememberVectorPainter(
                                if (filterExpanded) Icons.Filled.KeyboardArrowUp
                                else Icons.Filled.FilterList
                            ),
                            onClick = { action(UiAction.ExpandTextInput(!filterExpanded, cut.the.crap.ui.components.api.Screen.Posts)) },
                            contentDescription = stringResource(if (filterExpanded) Res.string.posts_cd_collapse_filters else Res.string.posts_cd_expand_filters),
                            showBadge = hasActiveFilters && !filterExpanded
                        )

                        if (isEciLoading) {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        } else {
                            MyIconAction(
                                iconPainter = rememberVectorPainter(
                                    Icons.Filled.BarChart
                                ),
                                onClick = { onLoadEciStatistics() },
                                contentDescription = stringResource(Res.string.posts_cd_load_stats)
                            )
                        }
                    },
                    navigationIcon = {
//                    FilledTonalIconButton(
//                        onClick = {
////                            showScreen(Screen.WellnessListOrigin)
//                        }) {
//                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
//                    }
                    }
                )
                AnimatedVisibility(filterExpanded) {
                    Column(modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 8.dp)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)  // Max height for approximately 3 rows
                                .verticalScroll(rememberScrollState())
                        ) {
                            FlowRow(modifier = Modifier.fillMaxWidth()) {
                                filterStateList.mapIndexed { index, item ->
                                    MyChip(
                                        onClick = { action(UiAction.ChipClicked(index, screen = cut.the.crap.ui.components.api.Screen.Posts)) },
                                        state = item,
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                        onLeadingClicked = if (item is FilterState.DateState && item.date != null) {
                                            { action(UiAction.ClearDateFilter(item.dateType, cut.the.crap.ui.components.api.Screen.Posts)) }
                                        } else null
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            }
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
        floatingActionButton = {
            if (!screenStateValue.selectionMode) {
            FloatingActionButton(
                onClick = {
                    action(cut.the.crap.ui.components.api.ContentItemAction.CreateNew)
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(Res.string.posts_cd_create_item)
                )
            }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            // Keep the editor mounted at all times. The filter panel expands as an
            // overlay in the top bar (see AnimatedVisibility above), so glancing at
            // filters must not swap out — and lose — the in-progress draft.
            ContentEditor(
                value = screenStateValue.focusedContentText,
                onValueChange = action,
                modifier = Modifier.padding(horizontal = 16.dp),
                selectedFileCount = screenStateValue.selectedFileUris.size,
                isUploading = screenStateValue.isUploading,
                onPickFiles = {
                    multipleFilesPicker.launch()
                },
                onUploadFiles = {
                    action(UploadAction.StartUpload)
                }
            )

            ContentList(
                action = action,
                paddingValues = paddingValues,
                contentItems = contentItems,
                onContentItemsReordered = onContentItemsReordered,
                selectionMode = screenStateValue.selectionMode,
                selectedItems = screenStateValue.selectedItems,
            )
        }
    }
}
