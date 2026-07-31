package cut.the.crap.ui.content.settings

import cut.the.crap.platform.rememberFilePicker

import cut.the.crap.platform.LoginFlow
import cut.the.crap.platform.LoginReason
import cut.the.crap.platform.Notifier
import cut.the.crap.platform.PlatformUri

import org.koin.compose.koinInject

import org.jetbrains.compose.resources.getString

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.color_picker_confirm
import cut.the.crap.shared.resources.color_picker_hex_label
import cut.the.crap.shared.resources.color_picker_title
import cut.the.crap.shared.resources.dialog_cancel
import cut.the.crap.shared.resources.dialog_ok
import cut.the.crap.shared.resources.settings_about
import cut.the.crap.shared.resources.settings_accent_color
import cut.the.crap.shared.resources.settings_appearance
import cut.the.crap.shared.resources.settings_backup_database
import cut.the.crap.shared.resources.settings_backup_database_desc
import cut.the.crap.shared.resources.settings_backup_frequency
import cut.the.crap.shared.resources.settings_backup_retention
import cut.the.crap.shared.resources.settings_clear
import cut.the.crap.shared.resources.settings_data_management
import cut.the.crap.shared.resources.settings_default_date_range
import cut.the.crap.shared.resources.settings_default_favorite_filter
import cut.the.crap.shared.resources.settings_default_sort_order
import cut.the.crap.shared.resources.settings_developer_options
import cut.the.crap.shared.resources.settings_dialog_links_date_range
import cut.the.crap.shared.resources.settings_dialog_links_favorite_filter
import cut.the.crap.shared.resources.settings_dialog_links_sort_order
import cut.the.crap.shared.resources.settings_dialog_posts_date_range
import cut.the.crap.shared.resources.settings_dialog_posts_favorite_filter
import cut.the.crap.shared.resources.settings_dialog_posts_sort_order
import cut.the.crap.shared.resources.settings_disable_developer_mode
import cut.the.crap.shared.resources.settings_disable_developer_mode_desc
import cut.the.crap.shared.resources.settings_display
import cut.the.crap.shared.resources.settings_edit_shared_link
import cut.the.crap.shared.resources.settings_edit_shared_link_desc
import cut.the.crap.shared.resources.settings_identity
import cut.the.crap.shared.resources.settings_identity_desc
import cut.the.crap.shared.resources.settings_import_export
import cut.the.crap.shared.resources.settings_import_export_desc
import cut.the.crap.shared.resources.settings_links_screen
import cut.the.crap.shared.resources.settings_manage_backups
import cut.the.crap.shared.resources.settings_manage_backups_desc
import cut.the.crap.shared.resources.settings_posts_screen
import cut.the.crap.shared.resources.settings_restore_body
import cut.the.crap.shared.resources.settings_restore_confirm
import cut.the.crap.shared.resources.settings_restore_database
import cut.the.crap.shared.resources.settings_restore_database_desc
import cut.the.crap.shared.resources.settings_restore_title
import cut.the.crap.shared.resources.settings_sharing
import cut.the.crap.shared.resources.settings_show_performance_metrics
import cut.the.crap.shared.resources.settings_show_performance_metrics_desc
import cut.the.crap.shared.resources.settings_theme
import cut.the.crap.shared.resources.settings_timestamp_format
import cut.the.crap.shared.resources.settings_title
import cut.the.crap.shared.resources.settings_toast_x_credentials_cleared
import cut.the.crap.shared.resources.settings_toast_x_credentials_saved
import cut.the.crap.shared.resources.settings_version
import cut.the.crap.shared.resources.settings_x_api_credentials
import cut.the.crap.shared.resources.settings_x_cookies_help
import cut.the.crap.shared.resources.settings_x_cookies_hint
import cut.the.crap.shared.resources.settings_x_edit_manually
import cut.the.crap.shared.resources.settings_x_enter_manually
import cut.the.crap.shared.resources.settings_x_logged_in
import cut.the.crap.shared.resources.settings_x_manual_credentials
import cut.the.crap.shared.resources.settings_x_not_logged_in
import androidx.compose.ui.graphics.Color
import cut.the.crap.ui.components.BottomNavigationBar
import cut.the.crap.ui.content.settings.identity.rememberIdentitySupported
import org.koin.compose.viewmodel.koinViewModel
import cut.the.crap.ui.components.ColorHistoryViewModel
import cut.the.crap.ui.components.ColorPickerDialog
import cut.the.crap.ui.components.colorFromHex
import cut.the.crap.ui.components.toHexString
import cut.the.crap.ui.components.api.Action
import cut.the.crap.ui.components.api.FileAction
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
    val identitySupported = rememberIdentitySupported()
    val notifier: Notifier = koinInject()
    val loginFlow: LoginFlow = koinInject()
    var showPostsDateRangeDialog by remember { mutableStateOf(false) }
    var showLinksDateRangeDialog by remember { mutableStateOf(false) }
    var showPostsFavoriteFilterDialog by remember { mutableStateOf(false) }
    var showLinksFavoriteFilterDialog by remember { mutableStateOf(false) }
    var showPostsSortOrderDialog by remember { mutableStateOf(false) }
    var showLinksSortOrderDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showTimestampFormatDialog by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    // Demo state for the reusable ColorPicker. Not yet persisted or applied to the theme —
    // this is a live showcase of the component ahead of wiring it to a real target.
    var accentColorDemo by remember { mutableStateOf(Color(0xFF3A7BD5)) }
    // Persistent, shared colour-pick history feeding the picker's history strip.
    val colorHistoryViewModel: ColorHistoryViewModel = koinViewModel()
    val recentColorHexes by colorHistoryViewModel.recentColors.collectAsState()
    val recentColors = remember(recentColorHexes) { recentColorHexes.mapNotNull(::colorFromHex) }
    var showXCredentialsDialog by remember { mutableStateOf(false) }
    var showBackupFrequencyDialog by remember { mutableStateOf(false) }
    var showBackupRetentionDialog by remember { mutableStateOf(false) }
    var developerTapCount by remember { mutableIntStateOf(0) }
    var pendingRestoreUri by remember { mutableStateOf<PlatformUri?>(null) }

    // Database restore file picker (.db backup). Downloads exposes these as
    // octet-stream, so we accept any type and validate the contents on restore.
    val restoreFilePicker = rememberFilePicker(mimeTypes = listOf("*/*")) { uris ->
        // Empty if the user cancelled; a pick triggers the confirm dialog.
        pendingRestoreUri = uris.firstOrNull()
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text(stringResource(Res.string.settings_restore_title)) },
            text = {
                Text(stringResource(Res.string.settings_restore_body))
            },
            confirmButton = {
                TextButton(onClick = {
                    action(FileAction.RestoreDatabase(uri))
                    pendingRestoreUri = null
                }) { Text(stringResource(Res.string.settings_restore_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) { Text(stringResource(Res.string.dialog_cancel)) }
            }
        )
    }

    if (showPostsDateRangeDialog) {
        DateRangeDialog(
            title = stringResource(Res.string.settings_dialog_posts_date_range),
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
            title = stringResource(Res.string.settings_dialog_links_date_range),
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
            title = stringResource(Res.string.settings_dialog_posts_favorite_filter),
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
            title = stringResource(Res.string.settings_dialog_links_favorite_filter),
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
            title = stringResource(Res.string.settings_dialog_posts_sort_order),
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
            title = stringResource(Res.string.settings_dialog_links_sort_order),
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

    if (showColorPickerDialog) {
        ColorPickerDialog(
            initialColor = accentColorDemo,
            title = stringResource(Res.string.color_picker_title),
            confirmLabel = stringResource(Res.string.color_picker_confirm),
            dismissLabel = stringResource(Res.string.dialog_cancel),
            hexLabel = stringResource(Res.string.color_picker_hex_label),
            recentColors = recentColors,
            onDismiss = { showColorPickerDialog = false },
            onConfirm = { chosen ->
                accentColorDemo = chosen
                colorHistoryViewModel.recordColor(chosen.toHexString())
                showColorPickerDialog = false
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
                title = { Text(stringResource(Res.string.settings_title)) },
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
                SettingsSectionHeader(title = stringResource(Res.string.settings_posts_screen))
            }

            item {
                SettingsItem(
                    icon = Icons.Default.DateRange,
                    title = stringResource(Res.string.settings_default_date_range),
                    subtitle = stringResource(currentSettings.postsDateRangePreset.displayNameResId),
                    onClick = { showPostsDateRangeDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Favorite,
                    title = stringResource(Res.string.settings_default_favorite_filter),
                    subtitle = stringResource(currentSettings.postsFavoriteFilterPreset.displayNameResId),
                    onClick = { showPostsFavoriteFilterDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Sort,
                    title = stringResource(Res.string.settings_default_sort_order),
                    subtitle = stringResource(currentSettings.postsSortOrderPreset.displayNameResId),
                    onClick = { showPostsSortOrderDialog = true }
                )
            }

            // Links Screen Section
            item {
                SettingsSectionHeader(title = stringResource(Res.string.settings_links_screen))
            }

            item {
                SettingsItem(
                    icon = Icons.Default.DateRange,
                    title = stringResource(Res.string.settings_default_date_range),
                    subtitle = stringResource(currentSettings.linksDateRangePreset.displayNameResId),
                    onClick = { showLinksDateRangeDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Favorite,
                    title = stringResource(Res.string.settings_default_favorite_filter),
                    subtitle = stringResource(currentSettings.linksFavoriteFilterPreset.displayNameResId),
                    onClick = { showLinksFavoriteFilterDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Sort,
                    title = stringResource(Res.string.settings_default_sort_order),
                    subtitle = stringResource(currentSettings.linksSortOrderPreset.displayNameResId),
                    onClick = { showLinksSortOrderDialog = true }
                )
            }

            // Appearance Section
            item {
                SettingsSectionHeader(title = stringResource(Res.string.settings_appearance))
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = stringResource(Res.string.settings_theme),
                    subtitle = stringResource(currentSettings.themePreference.displayNameResId),
                    onClick = { showThemeDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.ColorLens,
                    title = stringResource(Res.string.settings_accent_color),
                    subtitle = "#${accentColorDemo.toHexString()}",
                    onClick = { showColorPickerDialog = true },
                    color = accentColorDemo
                )
            }

            // Display Section
            item {
                SettingsSectionHeader(title = stringResource(Res.string.settings_display))
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Schedule,
                    title = stringResource(Res.string.settings_timestamp_format),
                    subtitle = stringResource(currentSettings.timestampFormat.displayNameResId),
                    onClick = { showTimestampFormatDialog = true }
                )
            }

            // Identity. Only shown where the platform can actually hold a private key: iOS has
            // no CryptoProvider/IdentityKeyStore yet, so identityModule is not loaded there and
            // the screen would fail to resolve its view model (doc/IDENTITY_SPEC.md §3.1).
            if (identitySupported) {
                item {
                    SettingsSectionHeader(title = stringResource(Res.string.settings_identity))
                }
                item {
                    SettingsItem(
                        icon = Icons.Default.Key,
                        title = stringResource(Res.string.settings_identity),
                        subtitle = stringResource(Res.string.settings_identity_desc),
                        onClick = { navController.navigate("identity") }
                    )
                }
            }

            // Data Management Section
            item {
                SettingsSectionHeader(title = stringResource(Res.string.settings_data_management))
            }

            item {
                SettingsItem(
                    icon = Icons.Default.ImportExport,
                    title = stringResource(Res.string.settings_import_export),
                    subtitle = stringResource(Res.string.settings_import_export_desc),
                    onClick = {
                        navController.navigate("import_export")
                    }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = stringResource(Res.string.settings_backup_database),
                    subtitle = stringResource(Res.string.settings_backup_database_desc),
                    onClick = {
                        action(FileAction.BackupDatabase)
                    }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Restore,
                    title = stringResource(Res.string.settings_restore_database),
                    subtitle = stringResource(Res.string.settings_restore_database_desc),
                    onClick = {
                        restoreFilePicker.launch()
                    }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Schedule,
                    title = stringResource(Res.string.settings_backup_frequency),
                    subtitle = stringResource(currentSettings.backupFrequency.displayNameResId),
                    onClick = { showBackupFrequencyDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.DeleteSweep,
                    title = stringResource(Res.string.settings_backup_retention),
                    subtitle = stringResource(currentSettings.backupRetention.displayNameResId),
                    onClick = { showBackupRetentionDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.FolderOpen,
                    title = stringResource(Res.string.settings_manage_backups),
                    subtitle = stringResource(Res.string.settings_manage_backups_desc),
                    onClick = { navController.navigate("backup_management") }
                )
            }

            // Sharing Section
            item {
                SettingsSectionHeader(title = stringResource(Res.string.settings_sharing))
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Share,
                    title = stringResource(Res.string.settings_edit_shared_link),
                    subtitle = stringResource(Res.string.settings_edit_shared_link_desc),
                    checked = currentSettings.editSharedLinkBeforeSave,
                    onCheckedChange = { checked ->
                        onSettingsChanged(currentSettings.copy(editSharedLinkBeforeSave = checked))
                    }
                )
            }

            // About Section
            item {
                SettingsSectionHeader(title = stringResource(Res.string.settings_about))
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = stringResource(Res.string.settings_version),
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
                        title = stringResource(Res.string.settings_developer_options),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                item {
                    SettingsSwitchItem(
                        icon = Icons.Default.BugReport,
                        title = stringResource(Res.string.settings_show_performance_metrics),
                        subtitle = stringResource(Res.string.settings_show_performance_metrics_desc),
                        checked = currentSettings.showPerformanceMetrics,
                        onCheckedChange = { checked ->
                            onSettingsChanged(currentSettings.copy(showPerformanceMetrics = checked))
                        }
                    )
                }

                // Only offered where the platform can actually run the sign-in flow. On a
                // platform without a WebView there is no button here at all — the manual
                // credential item below is the way in.
                if (loginFlow.isSupported) {
                    item {
                        val hasCredentials = currentSettings.xAuthToken != null && currentSettings.xCt0Token != null
                        SettingsItem(
                            icon = Icons.Default.Key,
                            title = "X Login",
                            subtitle = if (hasCredentials) stringResource(Res.string.settings_x_logged_in) else stringResource(Res.string.settings_x_not_logged_in),
                            onClick = {
                                loginFlow.launch(
                                    if (hasCredentials) LoginReason.SessionExpired else LoginReason.InitialSetup
                                )
                            }
                        )
                    }
                }

                // Manual credential entry as fallback
                item {
                    val hasCredentials = currentSettings.xAuthToken != null && currentSettings.xCt0Token != null
                    SettingsItem(
                        icon = Icons.Default.Edit,
                        title = stringResource(Res.string.settings_x_manual_credentials),
                        subtitle = if (hasCredentials) stringResource(Res.string.settings_x_edit_manually) else stringResource(Res.string.settings_x_enter_manually),
                        onClick = { showXCredentialsDialog = true }
                    )
                }

                item {
                    SettingsItem(
                        icon = Icons.Default.DeveloperMode,
                        title = stringResource(Res.string.settings_disable_developer_mode),
                        subtitle = stringResource(Res.string.settings_disable_developer_mode_desc),
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
                Text(stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_cancel))
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
        title = { Text(stringResource(Res.string.settings_theme)) },
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
                Text(stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_cancel))
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
        title = { Text(stringResource(Res.string.settings_timestamp_format)) },
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
                Text(stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_cancel))
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
                Text(stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_cancel))
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
                Text(stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_cancel))
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
    val notifier: Notifier = koinInject()
    // Resolved in composition; the notifications below run in non-composable callbacks.
    val credentialsSavedMessage = stringResource(Res.string.settings_toast_x_credentials_saved)
    val credentialsClearedMessage = stringResource(Res.string.settings_toast_x_credentials_cleared)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_x_api_credentials)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.settings_x_cookies_help),
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
                    text = stringResource(Res.string.settings_x_cookies_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(authToken, ct0Token)
                    notifier.show(credentialsSavedMessage)
                }
            ) {
                Text(stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            Row {
                if (currentAuthToken.isNotBlank() || currentCt0Token.isNotBlank()) {
                    TextButton(
                        onClick = {
                            onConfirm("", "")
                            notifier.show(credentialsClearedMessage)
                        }
                    ) {
                        Text(stringResource(Res.string.settings_clear), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.dialog_cancel))
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
        title = { Text(stringResource(Res.string.settings_backup_frequency)) },
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
                Text(stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_cancel))
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
        title = { Text(stringResource(Res.string.settings_backup_retention)) },
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
                Text(stringResource(Res.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_cancel))
            }
        }
    )
}
