package cut.the.crap.ui.content.settings

import cut.the.crap.platform.Notifier

import org.koin.compose.koinInject

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getPluralString

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.action_back
import cut.the.crap.shared.resources.backup_cd_delete_selected
import cut.the.crap.shared.resources.backup_delete_body
import cut.the.crap.shared.resources.backup_delete_title
import cut.the.crap.shared.resources.backup_deleted
import cut.the.crap.shared.resources.backup_empty_message
import cut.the.crap.shared.resources.backup_empty_title
import cut.the.crap.shared.resources.dialog_cancel
import cut.the.crap.shared.resources.dialog_delete
import cut.the.crap.shared.resources.settings_manage_backups
import cut.the.crap.shared.resources.tag_dialog_selected_count
import org.koin.androidx.compose.koinViewModel
import androidx.navigation.NavHostController
import cut.the.crap.data.backup.BackupInfo
import cut.the.crap.ui.components.BottomNavigationBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupManagementScreen(
    navController: NavHostController,
    viewModel: BackupViewModel = koinViewModel()
) {
    val notifier: Notifier = koinInject()
    val scope = rememberCoroutineScope()
    val backups by viewModel.backups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Selected backup Uris. Cleared whenever the underlying list changes.
    var selectedUris by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Drop selections that no longer exist after a refresh/delete.
    LaunchedEffect(backups) {
        val availableUris = backups.map { it.uri }.toSet()
        selectedUris = selectedUris.intersect(availableUris)
    }

    val inSelectionMode = selectedUris.isNotEmpty()

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(Res.string.backup_delete_title)) },
            text = {
                Text(
                    pluralStringResource(
                        Res.plurals.backup_delete_body, selectedUris.size, selectedUris.size
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val toDelete = selectedUris.toList()
                    showDeleteConfirm = false
                    viewModel.deleteBackups(toDelete) { deleted ->
                        // The count is only known when the delete completes, so the plural
                        // can't be hoisted into composition; resolve it in a coroutine.
                        scope.launch {
                            notifier.show(getPluralString(Res.plurals.backup_deleted, deleted, deleted))
                        }
                    }
                    selectedUris = emptySet()
                }) { Text(stringResource(Res.string.dialog_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(Res.string.dialog_cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (inSelectionMode) stringResource(Res.string.tag_dialog_selected_count, selectedUris.size)
                        else stringResource(Res.string.settings_manage_backups)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (inSelectionMode) selectedUris = emptySet()
                        else navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back)
                        )
                    }
                },
                actions = {
                    if (inSelectionMode) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(Res.string.backup_cd_delete_selected)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                backups.isEmpty() -> {
                    EmptyBackups(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(backups, key = { it.uri.toString() }) { backup ->
                            BackupRow(
                                backup = backup,
                                selected = backup.uri in selectedUris,
                                onToggle = {
                                    selectedUris = if (backup.uri in selectedUris) {
                                        selectedUris - backup.uri
                                    } else {
                                        selectedUris + backup.uri
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupRow(
    backup: BackupInfo,
    selected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = backup.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${formatDate(backup.lastModified)} • ${formatSize(backup.sizeBytes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyBackups(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.FolderOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(Res.string.backup_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(Res.string.backup_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(epochMillis))

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.getDefault(), "%.1f MB", mb)
}
