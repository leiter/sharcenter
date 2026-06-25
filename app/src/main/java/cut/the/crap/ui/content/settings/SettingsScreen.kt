package cut.the.crap.ui.content.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import cut.the.crap.R
import cut.the.crap.ui.XLoginActivity
import cut.the.crap.ui.components.BottomNavigationBar
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.components.api.FileAction
import cut.the.crap.ui.theme.PreviewAppThemeProvider
import cut.the.crap.ui.theme.PreviewThemeWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    action: (Action) -> Unit,
    navController: NavHostController,
    settings: StateFlow<AppSettings>,
    onSettingsChanged: (AppSettings) -> Unit
) {
    val currentSettings by settings.collectAsState()
    val context = LocalContext.current
    var showPostsDateRangeDialog by remember { mutableStateOf(false) }
    var showLinksDateRangeDialog by remember { mutableStateOf(false) }
    var showPostsFavoriteFilterDialog by remember { mutableStateOf(false) }
    var showLinksFavoriteFilterDialog by remember { mutableStateOf(false) }
    var showPostsSortOrderDialog by remember { mutableStateOf(false) }
    var showLinksSortOrderDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showTimestampFormatDialog by remember { mutableStateOf(false) }
    var showXCredentialsDialog by remember { mutableStateOf(false) }
    var showBackupFrequencyDialog by remember { mutableStateOf(false) }
    var showBackupRetentionDialog by remember { mutableStateOf(false) }
    var developerTapCount by remember { mutableIntStateOf(0) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // X Login launcher
    val xLoginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == XLoginActivity.RESULT_LOGIN_SUCCESS) {
            Toast.makeText(context, "X login successful", Toast.LENGTH_SHORT).show()
        }
    }

    // Database restore file picker (.db backup). Downloads exposes these as
    // octet-stream, so we accept any type and validate the contents on restore.
    val restoreFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingRestoreUri = uri // null if the user cancelled; triggers confirm dialog
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("Restore database?") },
            text = {
                Text(
                    "This will replace ALL current data with the contents of the selected " +
                        "backup, then restart the app. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    action(FileAction.RestoreDatabase(uri))
                    pendingRestoreUri = null
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) { Text("Cancel") }
            }
        )
    }

    if (showPostsDateRangeDialog) {
        DateRangeDialog(
            title = stringResource(R.string.settings_dialog_posts_date_range),
            currentPreset = currentSettings.postsDateRangePreset,
            onDismiss = { showPostsDateRangeDialog = false },
            onConfirm = { preset ->
                onSettingsChanged(currentSettings.copy(postsDateRangePreset = preset))
                showPostsDateRangeDialog = false
            }
        )
    }

    if (showLinksDateRangeDialog) {
        DateRangeDialog(
            title = stringResource(R.string.settings_dialog_links_date_range),
            currentPreset = currentSettings.linksDateRangePreset,
            onDismiss = { showLinksDateRangeDialog = false },
            onConfirm = { preset ->
                onSettingsChanged(currentSettings.copy(linksDateRangePreset = preset))
                showLinksDateRangeDialog = false
            }
        )
    }

    if (showPostsFavoriteFilterDialog) {
        FavoriteFilterDialog(
            title = stringResource(R.string.settings_dialog_posts_favorite_filter),
            currentPreset = currentSettings.postsFavoriteFilterPreset,
            onDismiss = { showPostsFavoriteFilterDialog = false },
            onConfirm = { preset ->
                onSettingsChanged(currentSettings.copy(postsFavoriteFilterPreset = preset))
                showPostsFavoriteFilterDialog = false
            }
        )
    }

    if (showLinksFavoriteFilterDialog) {
        FavoriteFilterDialog(
            title = stringResource(R.string.settings_dialog_links_favorite_filter),
            currentPreset = currentSettings.linksFavoriteFilterPreset,
            onDismiss = { showLinksFavoriteFilterDialog = false },
            onConfirm = { preset ->
                onSettingsChanged(currentSettings.copy(linksFavoriteFilterPreset = preset))
                showLinksFavoriteFilterDialog = false
            }
        )
    }

    if (showPostsSortOrderDialog) {
        SortOrderDialog(
            title = stringResource(R.string.settings_dialog_posts_sort_order),
            currentPreset = currentSettings.postsSortOrderPreset,
            onDismiss = { showPostsSortOrderDialog = false },
            onConfirm = { preset ->
                onSettingsChanged(currentSettings.copy(postsSortOrderPreset = preset))
                showPostsSortOrderDialog = false
            }
        )
    }

    if (showLinksSortOrderDialog) {
        SortOrderDialog(
            title = stringResource(R.string.settings_dialog_links_sort_order),
            currentPreset = currentSettings.linksSortOrderPreset,
            onDismiss = { showLinksSortOrderDialog = false },
            onConfirm = { preset ->
                onSettingsChanged(currentSettings.copy(linksSortOrderPreset = preset))
                showLinksSortOrderDialog = false
            }
        )
    }

    if (showThemeDialog) {
        ThemeDialog(
            currentTheme = currentSettings.themePreference,
            onDismiss = { showThemeDialog = false },
            onConfirm = { theme ->
                onSettingsChanged(currentSettings.copy(themePreference = theme))
                showThemeDialog = false
            }
        )
    }

    if (showTimestampFormatDialog) {
        TimestampFormatDialog(
            currentFormat = currentSettings.timestampFormat,
            onDismiss = { showTimestampFormatDialog = false },
            onConfirm = { format ->
                onSettingsChanged(currentSettings.copy(timestampFormat = format))
                showTimestampFormatDialog = false
            }
        )
    }

    if (showBackupFrequencyDialog) {
        BackupFrequencyDialog(
            currentFrequency = currentSettings.backupFrequency,
            onDismiss = { showBackupFrequencyDialog = false },
            onConfirm = { frequency ->
                onSettingsChanged(currentSettings.copy(backupFrequency = frequency))
                showBackupFrequencyDialog = false
            }
        )
    }

    if (showBackupRetentionDialog) {
        BackupRetentionDialog(
            currentRetention = currentSettings.backupRetention,
            onDismiss = { showBackupRetentionDialog = false },
            onConfirm = { retention ->
                onSettingsChanged(currentSettings.copy(backupRetention = retention))
                showBackupRetentionDialog = false
            }
        )
    }

    if (showXCredentialsDialog) {
        XCredentialsDialog(
            currentAuthToken = currentSettings.xAuthToken ?: "",
            currentCt0Token = currentSettings.xCt0Token ?: "",
            onDismiss = { showXCredentialsDialog = false },
            onConfirm = { authToken, ct0Token ->
                onSettingsChanged(currentSettings.copy(
                    xAuthToken = authToken.takeIf { it.isNotBlank() },
                    xCt0Token = ct0Token.takeIf { it.isNotBlank() }
                ))
                showXCredentialsDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Posts Screen Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_posts_screen))
            }

            item {
                SettingsItem(
                    icon = Icons.Default.DateRange,
                    title = stringResource(R.string.settings_default_date_range),
                    subtitle = stringResource(currentSettings.postsDateRangePreset.displayNameResId),
                    onClick = { showPostsDateRangeDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Favorite,
                    title = stringResource(R.string.settings_default_favorite_filter),
                    subtitle = stringResource(currentSettings.postsFavoriteFilterPreset.displayNameResId),
                    onClick = { showPostsFavoriteFilterDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Sort,
                    title = stringResource(R.string.settings_default_sort_order),
                    subtitle = stringResource(currentSettings.postsSortOrderPreset.displayNameResId),
                    onClick = { showPostsSortOrderDialog = true }
                )
            }

            // Links Screen Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_links_screen))
            }

            item {
                SettingsItem(
                    icon = Icons.Default.DateRange,
                    title = stringResource(R.string.settings_default_date_range),
                    subtitle = stringResource(currentSettings.linksDateRangePreset.displayNameResId),
                    onClick = { showLinksDateRangeDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Favorite,
                    title = stringResource(R.string.settings_default_favorite_filter),
                    subtitle = stringResource(currentSettings.linksFavoriteFilterPreset.displayNameResId),
                    onClick = { showLinksFavoriteFilterDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Sort,
                    title = stringResource(R.string.settings_default_sort_order),
                    subtitle = stringResource(currentSettings.linksSortOrderPreset.displayNameResId),
                    onClick = { showLinksSortOrderDialog = true }
                )
            }

            // Appearance Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_appearance))
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.settings_theme),
                    subtitle = stringResource(currentSettings.themePreference.displayNameResId),
                    onClick = { showThemeDialog = true }
                )
            }

            // Display Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_display))
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Schedule,
                    title = stringResource(R.string.settings_timestamp_format),
                    subtitle = stringResource(currentSettings.timestampFormat.displayNameResId),
                    onClick = { showTimestampFormatDialog = true }
                )
            }

            // Data Management Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_data_management))
            }

            item {
                SettingsItem(
                    icon = Icons.Default.ImportExport,
                    title = stringResource(R.string.settings_import_export),
                    subtitle = stringResource(R.string.settings_import_export_desc),
                    onClick = {
                        navController.navigate("import_export")
                    }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = "Backup Database",
                    subtitle = "Create a manual backup of your database",
                    onClick = {
                        action(FileAction.BackupDatabase)
                    }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Restore,
                    title = "Restore Database",
                    subtitle = "Replace current data with a .db backup file",
                    onClick = {
                        restoreFilePickerLauncher.launch(arrayOf("*/*"))
                    }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Schedule,
                    title = stringResource(R.string.settings_backup_frequency),
                    subtitle = stringResource(currentSettings.backupFrequency.displayNameResId),
                    onClick = { showBackupFrequencyDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = stringResource(R.string.settings_backup_retention),
                    subtitle = stringResource(currentSettings.backupRetention.displayNameResId),
                    onClick = { showBackupRetentionDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.FolderOpen,
                    title = stringResource(R.string.settings_manage_backups),
                    subtitle = stringResource(R.string.settings_manage_backups_desc),
                    onClick = { navController.navigate("backup_management") }
                )
            }

            // Sharing Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_sharing))
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Share,
                    title = stringResource(R.string.settings_edit_shared_link),
                    subtitle = stringResource(R.string.settings_edit_shared_link_desc),
                    checked = currentSettings.editSharedLinkBeforeSave,
                    onCheckedChange = { checked ->
                        onSettingsChanged(currentSettings.copy(editSharedLinkBeforeSave = checked))
                    }
                )
            }

            // About Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_about))
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.settings_version),
                    subtitle = "1.0.0",
                    onClick = {
                        developerTapCount++
                        if (developerTapCount >= 7 && !currentSettings.developerMode) {
                            onSettingsChanged(currentSettings.copy(developerMode = true))
                            developerTapCount = 0
                        }
                    }
                )
            }

            // Developer Options (only shown if enabled)
            if (currentSettings.developerMode) {
                item {
                    SettingsSectionHeader(
                        title = stringResource(R.string.settings_developer_options),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                item {
                    SettingsSwitchItem(
                        icon = Icons.Default.BugReport,
                        title = stringResource(R.string.settings_show_performance_metrics),
                        subtitle = stringResource(R.string.settings_show_performance_metrics_desc),
                        checked = currentSettings.showPerformanceMetrics,
                        onCheckedChange = { checked ->
                            onSettingsChanged(currentSettings.copy(showPerformanceMetrics = checked))
                        }
                    )
                }

                item {
                    val hasCredentials = currentSettings.xAuthToken != null && currentSettings.xCt0Token != null
                    SettingsItem(
                        icon = Icons.Default.Key,
                        title = "X Login",
                        subtitle = if (hasCredentials) "Logged in - tap to re-login" else "Not logged in - tap to login",
                        onClick = {
                            val intent = XLoginActivity.createIntent(
                                context,
                                if (hasCredentials) XLoginActivity.REASON_AUTH_EXPIRED else XLoginActivity.REASON_INITIAL_SETUP
                            )
                            xLoginLauncher.launch(intent)
                        }
                    )
                }

                // Manual credential entry as fallback
                item {
                    val hasCredentials = currentSettings.xAuthToken != null && currentSettings.xCt0Token != null
                    SettingsItem(
                        icon = Icons.Default.Edit,
                        title = "Manual X Credentials",
                        subtitle = if (hasCredentials) "Edit manually" else "Enter cookies manually",
                        onClick = { showXCredentialsDialog = true }
                    )
                }

                item {
                    SettingsItem(
                        icon = Icons.Default.DeveloperMode,
                        title = stringResource(R.string.settings_disable_developer_mode),
                        subtitle = stringResource(R.string.settings_disable_developer_mode_desc),
                        onClick = {
                            onSettingsChanged(currentSettings.copy(
                                developerMode = false,
                                showPerformanceMetrics = false
                            ))
                        },
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = color
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun DateRangeDialog(
    title: String,
    currentPreset: DateRangePreset,
    onDismiss: () -> Unit,
    onConfirm: (DateRangePreset) -> Unit
) {
    var selectedPreset by remember { mutableStateOf(currentPreset) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                DateRangePreset.entries.filter { it != DateRangePreset.CUSTOM }.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPreset = preset }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPreset == preset,
                            onClick = { selectedPreset = preset }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(preset.displayNameResId))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedPreset) }) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
private fun ThemeDialog(
    currentTheme: ThemePreference,
    onDismiss: () -> Unit,
    onConfirm: (ThemePreference) -> Unit
) {
    var selectedTheme by remember { mutableStateOf(currentTheme) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme)) },
        text = {
            Column {
                ThemePreference.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTheme = theme }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTheme == theme,
                            onClick = { selectedTheme = theme }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(theme.displayNameResId))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedTheme) }) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
private fun TimestampFormatDialog(
    currentFormat: TimestampFormat,
    onDismiss: () -> Unit,
    onConfirm: (TimestampFormat) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(currentFormat) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_timestamp_format)) },
        text = {
            Column {
                TimestampFormat.entries.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFormat = format }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(format.displayNameResId))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedFormat) }) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
private fun FavoriteFilterDialog(
    title: String,
    currentPreset: FavoriteFilterPreset,
    onDismiss: () -> Unit,
    onConfirm: (FavoriteFilterPreset) -> Unit
) {
    var selectedPreset by remember { mutableStateOf(currentPreset) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                FavoriteFilterPreset.entries.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPreset = preset }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPreset == preset,
                            onClick = { selectedPreset = preset }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(preset.displayNameResId))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedPreset) }) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
private fun SortOrderDialog(
    title: String,
    currentPreset: SortOrderPreset,
    onDismiss: () -> Unit,
    onConfirm: (SortOrderPreset) -> Unit
) {
    var selectedPreset by remember { mutableStateOf(currentPreset) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                SortOrderPreset.entries.forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPreset = preset }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPreset == preset,
                            onClick = { selectedPreset = preset }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(preset.displayNameResId))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedPreset) }) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
private fun XCredentialsDialog(
    currentAuthToken: String,
    currentCt0Token: String,
    onDismiss: () -> Unit,
    onConfirm: (authToken: String, ct0Token: String) -> Unit
) {
    var authToken by remember { mutableStateOf(currentAuthToken) }
    var ct0Token by remember { mutableStateOf(currentCt0Token) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("X API Credentials") },
        text = {
            Column {
                Text(
                    text = "Enter your X (Twitter) cookies from Firefox to enable /i/status/ URL resolution.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = authToken,
                    onValueChange = { authToken = it },
                    label = { Text("auth_token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = ct0Token,
                    onValueChange = { ct0Token = it },
                    label = { Text("ct0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Get these from Firefox cookies for x.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(authToken, ct0Token)
                    Toast.makeText(context, "X credentials saved", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            Row {
                if (currentAuthToken.isNotBlank() || currentCt0Token.isNotBlank()) {
                    TextButton(
                        onClick = {
                            onConfirm("", "")
                            Toast.makeText(context, "X credentials cleared", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        }
    )
}

@Composable
private fun BackupFrequencyDialog(
    currentFrequency: BackupFrequency,
    onDismiss: () -> Unit,
    onConfirm: (BackupFrequency) -> Unit
) {
    var selected by remember { mutableStateOf(currentFrequency) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_backup_frequency)) },
        text = {
            Column {
                BackupFrequency.entries.forEach { frequency ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = frequency }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == frequency,
                            onClick = { selected = frequency }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(frequency.displayNameResId))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
private fun BackupRetentionDialog(
    currentRetention: BackupRetention,
    onDismiss: () -> Unit,
    onConfirm: (BackupRetention) -> Unit
) {
    var selected by remember { mutableStateOf(currentRetention) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_backup_retention)) },
        text = {
            Column {
                BackupRetention.entries.forEach { retention ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = retention }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == retention,
                            onClick = { selected = retention }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(retention.displayNameResId))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
private fun Preview(
    @PreviewParameter(PreviewAppThemeProvider::class) theme: PreviewThemeWrapper,
) {
    theme {
        SettingsScreen(
            action = {},
            navController = rememberNavController(),
            settings = MutableStateFlow(AppSettings(developerMode = true)),
            onSettingsChanged = {}
        )
    }
}
