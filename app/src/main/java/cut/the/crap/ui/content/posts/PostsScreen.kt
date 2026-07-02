package cut.the.crap.ui.content.posts

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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
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
import cut.the.crap.ui.theme.PreviewAppThemeProvider
import cut.the.crap.ui.theme.PreviewThemeWrapper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
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
    val context = LocalContext.current
    val isEciLoading by eciLoading.collectAsState()

    // Navigate to the statistics table on success, or toast the error.
    LaunchedEffect(Unit) {
        eciEvents.collect { event ->
            when (event) {
                EciUiEvent.NavigateToTable -> navController.navigate("eci_statistics")
                is EciUiEvent.ShowError ->
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Show a snackbar (with a Delete action) when the editor is cleared,
    // letting the user also delete the post that was detached from the editor.
    LaunchedEffect(Unit) {
        snackBarEvents.collect { event ->
            when (event) {
                is PostsSnackbarEvent.OfferDeleteClearedItem -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Post cleared",
                        actionLabel = "Delete",
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

    // File picker launcher for selecting multiple documents
    val multipleFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
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
                            contentDescription = if (filterExpanded) "Collapse filters" else "Expand filters",
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
                                    Icons.Filled.Key
                                ),
                                onClick = { onLoadEciStatistics() },
                                contentDescription = "Load initiative statistics"
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
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    action(cut.the.crap.ui.components.api.ContentItemAction.CreateNew)
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Create new content item"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            // Show ContentEditor when filter section is not expanded
            if (!filterExpanded) {
                ContentEditor(
                    value = screenStateValue.focusedContentText,
                    onValueChange = action,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    selectedFileCount = screenStateValue.selectedFileUris.size,
                    isUploading = screenStateValue.isUploading,
                    onPickFiles = {
                        multipleFilesLauncher.launch(arrayOf("*/*"))
                    },
                    onUploadFiles = {
                        action(UploadAction.StartUpload)
                    }
                )
            }

            ContentList(
                action = action,
                paddingValues = paddingValues,
                contentItems = contentItems,
                onContentItemsReordered = onContentItemsReordered,
            )
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
private fun Preview(
    @PreviewParameter(PreviewAppThemeProvider::class) theme: PreviewThemeWrapper,
) {
    theme {
        val contentItemsFlow = remember {
            MutableStateFlow(
                listOf(
                    ContentItem(
                        id = 1,
                        text = "First content item for posting",
                        isActive = true,
                        isFavorite = false,
                        lastModified = System.currentTimeMillis()
                    ),
                    ContentItem(
                        id = 2,
                        text = "Second content item",
                        isActive = false,
                        isFavorite = true,
                        lastModified = System.currentTimeMillis() - 86400000
                    ),
                    ContentItem(
                        id = 3,
                        text = "Third content item with longer text that demonstrates how the card handles multiple lines of content",
                        isActive = false,
                        isFavorite = false,
                        lastModified = System.currentTimeMillis() - 172800000
                    )
                )
            )
        }
        val screenStateFlow = remember {
            MutableStateFlow(PostsScreenState())
        }

        PostsScreen(
            action = {},
            navController = rememberNavController(),
            screenState = screenStateFlow,
            contentItems = contentItemsFlow,
            onContentItemsReordered = {}
        )
    }
}
