package cut.the.crap.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.dialog_cancel
import cut.the.crap.shared.resources.dialog_ok
import cut.the.crap.shared.resources.keyword_cd_clear_search
import cut.the.crap.shared.resources.keyword_cd_settings
import cut.the.crap.shared.resources.keyword_filter_all
import cut.the.crap.shared.resources.keyword_filter_favorites
import cut.the.crap.shared.resources.keyword_filter_non_favorites
import cut.the.crap.shared.resources.tag_dialog_add_handle
import cut.the.crap.shared.resources.tag_dialog_add_keyword
import cut.the.crap.shared.resources.tag_dialog_add_new
import cut.the.crap.shared.resources.tag_dialog_add_tag
import cut.the.crap.shared.resources.tag_dialog_add_to_favorites
import cut.the.crap.shared.resources.tag_dialog_back_to_list
import cut.the.crap.shared.resources.tag_dialog_button_add
import cut.the.crap.shared.resources.tag_dialog_button_delete
import cut.the.crap.shared.resources.tag_dialog_button_select_items
import cut.the.crap.shared.resources.tag_dialog_delete
import cut.the.crap.shared.resources.tag_dialog_delete_handles
import cut.the.crap.shared.resources.tag_dialog_delete_keywords
import cut.the.crap.shared.resources.tag_dialog_delete_mode
import cut.the.crap.shared.resources.tag_dialog_delete_tags
import cut.the.crap.shared.resources.tag_dialog_enter_handle
import cut.the.crap.shared.resources.tag_dialog_enter_tag
import cut.the.crap.shared.resources.tag_dialog_item_deleted
import cut.the.crap.shared.resources.tag_dialog_items_deleted
import cut.the.crap.shared.resources.tag_dialog_no_items
import cut.the.crap.shared.resources.tag_dialog_no_matching_items
import cut.the.crap.shared.resources.tag_dialog_not_selected
import cut.the.crap.shared.resources.tag_dialog_placeholder_handle
import cut.the.crap.shared.resources.tag_dialog_placeholder_keyword
import cut.the.crap.shared.resources.tag_dialog_placeholder_tag
import cut.the.crap.shared.resources.tag_dialog_prefix_help_handle
import cut.the.crap.shared.resources.tag_dialog_prefix_help_tag
import cut.the.crap.shared.resources.tag_dialog_remove_from_favorites
import cut.the.crap.shared.resources.tag_dialog_reverse_list
import cut.the.crap.shared.resources.tag_dialog_search
import cut.the.crap.shared.resources.tag_dialog_search_placeholder
import cut.the.crap.shared.resources.tag_dialog_select_handles
import cut.the.crap.shared.resources.tag_dialog_select_items
import cut.the.crap.shared.resources.tag_dialog_select_keywords
import cut.the.crap.shared.resources.tag_dialog_select_tags
import cut.the.crap.shared.resources.tag_dialog_selected
import cut.the.crap.shared.resources.tag_dialog_selected_count
import cut.the.crap.shared.resources.tag_dialog_selected_for_deletion
import cut.the.crap.shared.resources.tag_dialog_undo
import cut.the.crap.data.domain.KeyWord
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.components.api.ChipsType
import cut.the.crap.ui.components.api.KeywordAction

data class UndoState(
    val deletedItems: List<KeyWord>,
    val itemNames: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class FavoriteFilter {
    ALL,           // Show all items
    FAVORITES,     // Show only favorites
    NON_FAVORITES  // Show only non-favorites
}

@Composable
fun KeywordSelectionDialog(
    type: ChipsType,
    items: List<KeyWord>,
    selectedItems: Set<Int>,
    onItemToggle: (KeyWord) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onAdd: (Action) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddMode by remember { mutableStateOf(false) }
    var addText by remember { mutableStateOf("") }
    var deleteMode by remember { mutableStateOf(false) }
    var itemsToDelete by remember { mutableStateOf(setOf<Int>()) }
    var undoState by remember { mutableStateOf<UndoState?>(null) }
    var hiddenItemIds by remember { mutableStateOf(setOf<Int>()) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var favoriteFilter by remember { mutableStateOf(FavoriteFilter.ALL) }
    var reverseOrder by remember { mutableStateOf(false) }

    // Auto-dismiss undo banner after 5 seconds and actually delete items
    LaunchedEffect(undoState) {
        undoState?.let { undo ->
            kotlinx.coroutines.delay(5000)
            if (undoState?.timestamp == undo.timestamp) {
                // Timeout expired - actually delete the items from database
                val action = when (type) {
                    ChipsType.Handle -> {
                        if (undo.deletedItems.size == 1) {
                            KeywordAction.DeleteHandle(undo.deletedItems.first().id)
                        } else {
                            KeywordAction.DeleteHandlesBulk(undo.deletedItems.map { it.id })
                        }
                    }
                    ChipsType.Tag -> {
                        if (undo.deletedItems.size == 1) {
                            KeywordAction.DeleteTag(undo.deletedItems.first().id)
                        } else {
                            KeywordAction.DeleteTagsBulk(undo.deletedItems.map { it.id })
                        }
                    }
                    ChipsType.KeyWords -> {
                        if (undo.deletedItems.size == 1) {
                            KeywordAction.DeleteKeyWord(undo.deletedItems.first().id)
                        } else {
                            KeywordAction.DeleteKeyWordsBulk(undo.deletedItems.map { it.id })
                        }
                    }
                    else -> return@LaunchedEffect
                }
                onAdd(action)
                undoState = null
                hiddenItemIds = emptySet()
            }
        }
    }

    val title = when {
        showAddMode && type == ChipsType.Handle -> stringResource(Res.string.tag_dialog_add_handle)
        showAddMode && type == ChipsType.Tag -> stringResource(Res.string.tag_dialog_add_tag)
        showAddMode && type == ChipsType.KeyWords -> stringResource(Res.string.tag_dialog_add_keyword)
        deleteMode && type == ChipsType.Handle -> stringResource(Res.string.tag_dialog_delete_handles)
        deleteMode && type == ChipsType.Tag -> stringResource(Res.string.tag_dialog_delete_tags)
        deleteMode && type == ChipsType.KeyWords -> stringResource(Res.string.tag_dialog_delete_keywords)
        type == ChipsType.Handle -> stringResource(Res.string.tag_dialog_select_handles)
        type == ChipsType.Tag -> stringResource(Res.string.tag_dialog_select_tags)
        type == ChipsType.KeyWords -> stringResource(Res.string.tag_dialog_select_keywords)
        else -> stringResource(Res.string.tag_dialog_select_items)
    }

    val placeholder = when (type) {
        ChipsType.Handle -> stringResource(Res.string.tag_dialog_placeholder_handle)
        ChipsType.Tag -> stringResource(Res.string.tag_dialog_placeholder_tag)
        ChipsType.KeyWords -> stringResource(Res.string.tag_dialog_placeholder_keyword)
        else -> ""
    }

    // Filter items based on search query and favorite filter (don't filter hidden items here)
    val filteredItems = remember(items, searchQuery, favoriteFilter) {
        val searchFiltered = if (searchQuery.isBlank()) {
            items
        } else {
            items.filter { it.text.contains(searchQuery, ignoreCase = true) }
        }

        // Apply favorite filter
        when (favoriteFilter) {
            FavoriteFilter.ALL -> searchFiltered
            FavoriteFilter.FAVORITES -> searchFiltered.filter { it.isFavorite }
            FavoriteFilter.NON_FAVORITES -> searchFiltered.filter { !it.isFavorite }
        }
    }

    // Sort items: selected first, then by usage count
    val sortedItems = remember(filteredItems, selectedItems, reverseOrder) {
        val sorted = filteredItems.sortedWith(
            compareByDescending<KeyWord> { selectedItems.contains(it.id) }
                .thenByDescending { it.isFavorite }
                .thenByDescending { it.usageCount }
                .thenByDescending { it.lastUsed }
        )
        if (reverseOrder) sorted.reversed() else sorted
    }

    // Show search field if more than 5 items
    val showSearch = items.size > 5

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showAddMode || deleteMode) {
                    IconButton(onClick = {
                        showAddMode = false
                        deleteMode = false
                        addText = ""
                        itemsToDelete = emptySet()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.tag_dialog_back_to_list))
                    }
                }
                Text(text = title, modifier = Modifier.weight(1f))
                if (!showAddMode && !deleteMode) {
                    // Add button (second from right)
                    IconButton(onClick = { showAddMode = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(Res.string.tag_dialog_add_new))
                    }

                    // Overflow menu (rightmost position)
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(Res.string.keyword_cd_settings)
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            // Delete mode option
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.tag_dialog_delete_mode)) },
                                onClick = {
                                    deleteMode = true
                                    showOverflowMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null
                                    )
                                }
                            )

                            // Favorite filter toggle option
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (favoriteFilter) {
                                            FavoriteFilter.ALL -> stringResource(Res.string.keyword_filter_all)
                                            FavoriteFilter.FAVORITES -> stringResource(Res.string.keyword_filter_favorites)
                                            FavoriteFilter.NON_FAVORITES -> stringResource(Res.string.keyword_filter_non_favorites)
                                        }
                                    )
                                },
                                onClick = {
                                    favoriteFilter = when (favoriteFilter) {
                                        FavoriteFilter.ALL -> FavoriteFilter.FAVORITES
                                        FavoriteFilter.FAVORITES -> FavoriteFilter.NON_FAVORITES
                                        FavoriteFilter.NON_FAVORITES -> FavoriteFilter.ALL
                                    }
                                    showOverflowMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (favoriteFilter) {
                                            FavoriteFilter.ALL -> Icons.Default.Star
                                            FavoriteFilter.FAVORITES -> Icons.Default.Star
                                            FavoriteFilter.NON_FAVORITES -> Icons.Outlined.StarBorder
                                        },
                                        contentDescription = null
                                    )
                                }
                            )

                            // Reverse list option
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.tag_dialog_reverse_list)) },
                                onClick = {
                                    reverseOrder = !reverseOrder
                                    showOverflowMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Undo banner
                undoState?.let { undo ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = undo.itemNames,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    // Restore items from cache - just un hide them
                                    hiddenItemIds = hiddenItemIds - undo.deletedItems.map { it.id }.toSet()
                                    undoState = null
                                }
                            ) {
                                Text(
                                    text = stringResource(Res.string.tag_dialog_undo),
                                    color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }

                if (showAddMode) {
                    // Add mode UI
                    OutlinedTextField(
                        value = addText,
                        onValueChange = { addText = it },
                        label = {
                            Text(
                                if (type == ChipsType.Handle)
                                    stringResource(Res.string.tag_dialog_enter_handle)
                                else
                                    stringResource(Res.string.tag_dialog_enter_tag)
                            )
                        },
                        placeholder = { Text(placeholder) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (type == ChipsType.Handle)
                            stringResource(Res.string.tag_dialog_prefix_help_handle)
                        else
                            stringResource(Res.string.tag_dialog_prefix_help_tag),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (deleteMode) {
                    // Delete mode UI
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(Res.string.tag_dialog_search_placeholder)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(Res.string.tag_dialog_search))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = stringResource(Res.string.keyword_cd_clear_search)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (sortedItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank())
                                    stringResource(Res.string.tag_dialog_no_items)
                                else
                                    stringResource(Res.string.tag_dialog_no_matching_items),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            items(sortedItems, key = { it.id }) { item ->
                                val isMarkedForDeletion = itemsToDelete.contains(item.id)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            itemsToDelete = if (isMarkedForDeletion) {
                                                itemsToDelete - item.id
                                            } else {
                                                itemsToDelete + item.id
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isMarkedForDeletion)
                                            MaterialTheme.colorScheme.errorContainer
                                        else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (isMarkedForDeletion)
                                                    Icons.Filled.CheckCircle
                                                else
                                                    Icons.Outlined.Circle,
                                                contentDescription = if (isMarkedForDeletion)
                                                    stringResource(Res.string.tag_dialog_selected)
                                                else
                                                    stringResource(Res.string.tag_dialog_not_selected),
                                                tint = if (isMarkedForDeletion)
                                                    MaterialTheme.colorScheme.onErrorContainer
                                                else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(end = 12.dp)
                                            )
                                            Text(
                                                text = item.text,
                                                color = if (isMarkedForDeletion)
                                                    MaterialTheme.colorScheme.onErrorContainer
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                onAdd(KeywordAction.ToggleFavorite(item.id))
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                                contentDescription = if (item.isFavorite)
                                                    stringResource(Res.string.tag_dialog_remove_from_favorites)
                                                else
                                                    stringResource(Res.string.tag_dialog_add_to_favorites),
                                                tint = if (item.isFavorite) {
                                                    if (isMarkedForDeletion) MaterialTheme.colorScheme.onErrorContainer else Color(0xFFFFD700)
                                                } else {
                                                    if (isMarkedForDeletion) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (itemsToDelete.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.tag_dialog_selected_for_deletion, itemsToDelete.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    // Selection mode UI
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(Res.string.tag_dialog_search_placeholder)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(Res.string.tag_dialog_search)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = stringResource(Res.string.keyword_cd_clear_search)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (sortedItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank())
                                    stringResource(Res.string.tag_dialog_no_items)
                                else
                                    stringResource(Res.string.tag_dialog_no_matching_items),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            items(
                                items = sortedItems,
                                key = { it.id }
                            ) { item ->
                                val isSelected = selectedItems.contains(item.id)
                                val isHidden = hiddenItemIds.contains(item.id)
                                val deletedText = stringResource(
                                    Res.string.tag_dialog_item_deleted, item.text
                                )

                                // Use key to force recreation when hidden state changes
                                key(isHidden) {
                                    AnimatedVisibility(
                                        visible = !isHidden,
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        // Swipe-to-delete state (without deprecated confirmValueChange)
                                        val dismissState = rememberSwipeToDismissBoxState()

                                        // Observe dismiss state and handle the action
                                        LaunchedEffect(dismissState.currentValue) {
                                            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                                // Hide item immediately (don't delete from DB yet)
                                                hiddenItemIds = hiddenItemIds + item.id

                                                // Show undo banner with cached item
                                                undoState = UndoState(
                                                    deletedItems = listOf(item),
                                                    itemNames = deletedText
                                                )
                                            }
                                        }

                                        SwipeToDismissBox(
                                            state = dismissState,
                                            backgroundContent = {
                                                val backgroundColor = when (dismissState.dismissDirection) {
                                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                                                    else -> Color.Transparent
                                                }

                                                val iconTint = when (dismissState.dismissDirection) {
                                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onError
                                                    else -> Color.Transparent
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(backgroundColor)
                                                        .padding(horizontal = 20.dp),
                                                    contentAlignment = Alignment.CenterEnd
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = stringResource(Res.string.tag_dialog_delete),
                                                        tint = iconTint
                                                    )
                                                }
                                            },
                                            enableDismissFromStartToEnd = false
                                        ) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clickable { onItemToggle(item) },
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected)
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.surface
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = item.text,
                                                        color = if (isSelected)
                                                            MaterialTheme.colorScheme.onPrimaryContainer
                                                        else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            onAdd(KeywordAction.ToggleFavorite(item.id))
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                                            contentDescription = if (item.isFavorite)
                                                                stringResource(Res.string.tag_dialog_remove_from_favorites)
                                                            else
                                                                stringResource(Res.string.tag_dialog_add_to_favorites),
                                                            tint = if (item.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectedItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.tag_dialog_selected_count, selectedItems.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Full-width button for Add and Delete modes only
                if (showAddMode || deleteMode) {
                    Spacer(modifier = Modifier.height(16.dp))

                    val buttonText = when {
                        showAddMode -> stringResource(Res.string.tag_dialog_button_add)
                        deleteMode -> if (itemsToDelete.isNotEmpty())
                            stringResource(Res.string.tag_dialog_button_delete, itemsToDelete.size)
                        else
                            stringResource(Res.string.tag_dialog_button_select_items)

                        else -> ""
                    }

                    val buttonEnabled = when {
                        showAddMode -> addText.isNotBlank()
                        deleteMode -> itemsToDelete.isNotEmpty()
                        else -> false
                    }

                    // Get the actual items to delete
                    val itemsToDeleteList = items.filter { itemsToDelete.contains(it.id) }

                    val deletedNames = if (itemsToDeleteList.size == 1) {
                        stringResource(Res.string.tag_dialog_item_deleted, itemsToDeleteList.first().text)
                    } else {
                        stringResource(Res.string.tag_dialog_items_deleted, itemsToDeleteList.size)
                    }

                    Button(
                        onClick = {
                            when {
                                showAddMode -> {
                                    if (addText.isNotBlank()) {
                                        val formattedText = when (type) {
                                            ChipsType.Handle -> if (!addText.startsWith("@")) "@$addText" else addText
                                            ChipsType.Tag -> if (!addText.startsWith("#")) "#$addText" else addText
                                            ChipsType.KeyWords -> addText  // No prefix for keywords
                                            else -> addText
                                        }
                                        when (type) {
                                            ChipsType.Handle -> onAdd(KeywordAction.AddHandle(formattedText))
                                            ChipsType.Tag -> onAdd(KeywordAction.AddTag(formattedText))
                                            ChipsType.KeyWords -> onAdd(KeywordAction.AddKeyWord(formattedText))
                                            else -> {}
                                        }
                                        showAddMode = false
                                        addText = ""
                                    }
                                }
                                deleteMode -> {

                                    // Hide items immediately (don't delete from DB yet)
                                    hiddenItemIds = hiddenItemIds + itemsToDelete

                                    // Show undo banner with cached items

                                    undoState = UndoState(
                                        deletedItems = itemsToDeleteList,
                                        itemNames = deletedNames
                                    )

                                    deleteMode = false
                                    itemsToDelete = emptySet()
                                }
                            }
                        },
                        enabled = buttonEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (deleteMode && itemsToDelete.isNotEmpty()) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        }
                    ) {
                        Text(buttonText)
                    }
                }
            }
        },
        confirmButton = {
            if (!showAddMode && !deleteMode) {
                TextButton(
                    onClick = onConfirm,
                    enabled = selectedItems.isNotEmpty()
                ) {
                    Text(stringResource(Res.string.dialog_ok))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (showAddMode) {
                    showAddMode = false
                    addText = ""
                } else if (deleteMode) {
                    deleteMode = false
                    itemsToDelete = emptySet()
                } else {
                    onDismiss()
                }
            }) {
                Text(stringResource(Res.string.dialog_cancel))
            }
        }
    )
}
