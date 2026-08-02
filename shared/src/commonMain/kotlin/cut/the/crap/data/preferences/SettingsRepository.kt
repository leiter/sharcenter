package cut.the.crap.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import cut.the.crap.ui.content.settings.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.IOException

/** The name of the store this repository owns. Used by DI to build the singleton. */
const val SETTINGS_STORE = "app_settings"

/**
 * The DataStore is injected rather than derived from a Context: `preferencesDataStore(name = …)`
 * is an Android-only delegate, and it was also the thing that quietly made the store a singleton.
 * Injecting it makes that requirement explicit — DataStore throws if two live instances share a
 * file, so the store must be a Koin `single` even though this repository is a `factory`.
 */
class SettingsRepository constructor(
    private val dataStore: DataStore<Preferences>
) {
    // Preference keys
    private object PreferencesKeys {
        val DATE_RANGE_PRESET = stringPreferencesKey("date_range_preset") // Deprecated - kept for migration
        val POSTS_DATE_RANGE_PRESET = stringPreferencesKey("posts_date_range_preset")
        val LINKS_DATE_RANGE_PRESET = stringPreferencesKey("links_date_range_preset")
        val POSTS_FAVORITE_FILTER_PRESET = stringPreferencesKey("posts_favorite_filter_preset")
        val LINKS_FAVORITE_FILTER_PRESET = stringPreferencesKey("links_favorite_filter_preset")
        val POSTS_SORT_ORDER_PRESET = stringPreferencesKey("posts_sort_order_preset")
        val LINKS_SORT_ORDER_PRESET = stringPreferencesKey("links_sort_order_preset")
        val CUSTOM_START_DATE = longPreferencesKey("custom_start_date")
        val CUSTOM_END_DATE = longPreferencesKey("custom_end_date")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val TIMESTAMP_FORMAT = stringPreferencesKey("timestamp_format")
        val ITEMS_PER_LOAD = intPreferencesKey("items_per_load")
        val SHOW_FAVORITES_ONLY = booleanPreferencesKey("show_favorites_only") // Deprecated - kept for migration
        val AUTO_HIDE_OLD_ITEMS_DAYS = intPreferencesKey("auto_hide_old_items_days")
        val DEVELOPER_MODE = booleanPreferencesKey("developer_mode")
        val SHOW_PERFORMANCE_METRICS = booleanPreferencesKey("show_performance_metrics")
        val X_AUTH_TOKEN = stringPreferencesKey("x_auth_token")
        val X_CT0_TOKEN = stringPreferencesKey("x_ct0_token")
        val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
        val BACKUP_RETENTION = stringPreferencesKey("backup_retention")
        val EDIT_SHARED_LINK_BEFORE_SAVE = booleanPreferencesKey("edit_shared_link_before_save")
    }

    // Flow to read settings
    val settingsFlow: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            // Migration: if old key exists but new keys don't, use old value for both
            val legacyDateRange = preferences[PreferencesKeys.DATE_RANGE_PRESET]
            val defaultPreset = legacyDateRange?.let { DateRangePreset.valueOf(it) } ?: DateRangePreset.SEVEN_DAYS

            // Migration: if old showFavoritesOnly exists, convert to new preset
            val legacyShowFavoritesOnly = preferences[PreferencesKeys.SHOW_FAVORITES_ONLY]
            val defaultFavoriteFilter = if (legacyShowFavoritesOnly == true) {
                FavoriteFilterPreset.FAVORITES_ONLY
            } else {
                FavoriteFilterPreset.ALL
            }

            AppSettings(
                dateRangePreset = defaultPreset, // Kept for backward compatibility
                postsDateRangePreset = DateRangePreset.valueOf(
                    preferences[PreferencesKeys.POSTS_DATE_RANGE_PRESET] ?: defaultPreset.name
                ),
                linksDateRangePreset = DateRangePreset.valueOf(
                    preferences[PreferencesKeys.LINKS_DATE_RANGE_PRESET] ?: defaultPreset.name
                ),
                postsFavoriteFilterPreset = FavoriteFilterPreset.valueOf(
                    preferences[PreferencesKeys.POSTS_FAVORITE_FILTER_PRESET] ?: defaultFavoriteFilter.name
                ),
                linksFavoriteFilterPreset = FavoriteFilterPreset.valueOf(
                    preferences[PreferencesKeys.LINKS_FAVORITE_FILTER_PRESET] ?: defaultFavoriteFilter.name
                ),
                postsSortOrderPreset = SortOrderPreset.valueOf(
                    preferences[PreferencesKeys.POSTS_SORT_ORDER_PRESET] ?: SortOrderPreset.BY_DATE.name
                ),
                linksSortOrderPreset = SortOrderPreset.valueOf(
                    preferences[PreferencesKeys.LINKS_SORT_ORDER_PRESET] ?: SortOrderPreset.BY_DATE.name
                ),
                customStartDate = preferences[PreferencesKeys.CUSTOM_START_DATE],
                customEndDate = preferences[PreferencesKeys.CUSTOM_END_DATE],
                themePreference = ThemePreference.valueOf(
                    preferences[PreferencesKeys.THEME_PREFERENCE] ?: ThemePreference.SYSTEM.name
                ),
                timestampFormat = TimestampFormat.valueOf(
                    preferences[PreferencesKeys.TIMESTAMP_FORMAT] ?: TimestampFormat.RELATIVE.name
                ),
                itemsPerLoad = preferences[PreferencesKeys.ITEMS_PER_LOAD] ?: 50,
                showFavoritesOnly = preferences[PreferencesKeys.SHOW_FAVORITES_ONLY] ?: false,
                autoHideOldItemsDays = preferences[PreferencesKeys.AUTO_HIDE_OLD_ITEMS_DAYS],
                developerMode = preferences[PreferencesKeys.DEVELOPER_MODE] ?: false,
                showPerformanceMetrics = preferences[PreferencesKeys.SHOW_PERFORMANCE_METRICS] ?: false,
                xAuthToken = preferences[PreferencesKeys.X_AUTH_TOKEN],
                xCt0Token = preferences[PreferencesKeys.X_CT0_TOKEN],
                backupFrequency = BackupFrequency.valueOf(
                    preferences[PreferencesKeys.BACKUP_FREQUENCY] ?: BackupFrequency.DAILY.name
                ),
                backupRetention = BackupRetention.valueOf(
                    preferences[PreferencesKeys.BACKUP_RETENTION] ?: BackupRetention.KEEP_ALL.name
                ),
                editSharedLinkBeforeSave = preferences[PreferencesKeys.EDIT_SHARED_LINK_BEFORE_SAVE] ?: false
            )
        }

    // Save settings
    suspend fun updateSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.POSTS_DATE_RANGE_PRESET] = settings.postsDateRangePreset.name
            preferences[PreferencesKeys.LINKS_DATE_RANGE_PRESET] = settings.linksDateRangePreset.name
            preferences[PreferencesKeys.POSTS_FAVORITE_FILTER_PRESET] = settings.postsFavoriteFilterPreset.name
            preferences[PreferencesKeys.LINKS_FAVORITE_FILTER_PRESET] = settings.linksFavoriteFilterPreset.name
            preferences[PreferencesKeys.POSTS_SORT_ORDER_PRESET] = settings.postsSortOrderPreset.name
            preferences[PreferencesKeys.LINKS_SORT_ORDER_PRESET] = settings.linksSortOrderPreset.name
            settings.customStartDate?.let {
                preferences[PreferencesKeys.CUSTOM_START_DATE] = it
            }
            settings.customEndDate?.let {
                preferences[PreferencesKeys.CUSTOM_END_DATE] = it
            }
            preferences[PreferencesKeys.THEME_PREFERENCE] = settings.themePreference.name
            preferences[PreferencesKeys.TIMESTAMP_FORMAT] = settings.timestampFormat.name
            preferences[PreferencesKeys.ITEMS_PER_LOAD] = settings.itemsPerLoad
            preferences[PreferencesKeys.SHOW_FAVORITES_ONLY] = settings.showFavoritesOnly
            settings.autoHideOldItemsDays?.let {
                preferences[PreferencesKeys.AUTO_HIDE_OLD_ITEMS_DAYS] = it
            }
            preferences[PreferencesKeys.DEVELOPER_MODE] = settings.developerMode
            preferences[PreferencesKeys.SHOW_PERFORMANCE_METRICS] = settings.showPerformanceMetrics
            settings.xAuthToken?.let { preferences[PreferencesKeys.X_AUTH_TOKEN] = it }
            settings.xCt0Token?.let { preferences[PreferencesKeys.X_CT0_TOKEN] = it }
            preferences[PreferencesKeys.BACKUP_FREQUENCY] = settings.backupFrequency.name
            preferences[PreferencesKeys.BACKUP_RETENTION] = settings.backupRetention.name
            preferences[PreferencesKeys.EDIT_SHARED_LINK_BEFORE_SAVE] = settings.editSharedLinkBeforeSave
        }
    }

    // Individual update methods for convenience
    suspend fun updateDateRangePreset(preset: DateRangePreset) {
        // Deprecated - update both for backward compatibility
        dataStore.edit {
            it[PreferencesKeys.POSTS_DATE_RANGE_PRESET] = preset.name
            it[PreferencesKeys.LINKS_DATE_RANGE_PRESET] = preset.name
        }
    }

    suspend fun updatePostsDateRangePreset(preset: DateRangePreset) {
        dataStore.edit { it[PreferencesKeys.POSTS_DATE_RANGE_PRESET] = preset.name }
    }

    suspend fun updateLinksDateRangePreset(preset: DateRangePreset) {
        dataStore.edit { it[PreferencesKeys.LINKS_DATE_RANGE_PRESET] = preset.name }
    }

    suspend fun updatePostsFavoriteFilterPreset(preset: FavoriteFilterPreset) {
        dataStore.edit { it[PreferencesKeys.POSTS_FAVORITE_FILTER_PRESET] = preset.name }
    }

    suspend fun updateLinksFavoriteFilterPreset(preset: FavoriteFilterPreset) {
        dataStore.edit { it[PreferencesKeys.LINKS_FAVORITE_FILTER_PRESET] = preset.name }
    }

    suspend fun updatePostsSortOrderPreset(preset: SortOrderPreset) {
        dataStore.edit { it[PreferencesKeys.POSTS_SORT_ORDER_PRESET] = preset.name }
    }

    suspend fun updateLinksSortOrderPreset(preset: SortOrderPreset) {
        dataStore.edit { it[PreferencesKeys.LINKS_SORT_ORDER_PRESET] = preset.name }
    }

    suspend fun updateThemePreference(theme: ThemePreference) {
        dataStore.edit { it[PreferencesKeys.THEME_PREFERENCE] = theme.name }
    }

    suspend fun updateDeveloperMode(enabled: Boolean) {
        dataStore.edit { it[PreferencesKeys.DEVELOPER_MODE] = enabled }
    }

    suspend fun updateXCredentials(authToken: String?, ct0Token: String?) {
        dataStore.edit {
            if (authToken != null) {
                it[PreferencesKeys.X_AUTH_TOKEN] = authToken
            } else {
                it.remove(PreferencesKeys.X_AUTH_TOKEN)
            }
            if (ct0Token != null) {
                it[PreferencesKeys.X_CT0_TOKEN] = ct0Token
            } else {
                it.remove(PreferencesKeys.X_CT0_TOKEN)
            }
        }
    }
}
