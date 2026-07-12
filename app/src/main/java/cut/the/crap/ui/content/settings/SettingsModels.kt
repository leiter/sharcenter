package cut.the.crap.ui.content.settings

import cut.the.crap.R

/**
 * Date range presets for filtering content
 */
enum class DateRangePreset(val displayNameResId: Int, val days: Int?) {
    SEVEN_DAYS(R.string.date_range_preset_7_days, 7),
    THIRTY_DAYS(R.string.date_range_preset_30_days, 30),
    NINETY_DAYS(R.string.date_range_preset_90_days, 90),
    ALL_TIME(R.string.date_range_preset_all_time, null),
    CUSTOM(R.string.date_range_preset_custom, -1); // -1 indicates custom range

    companion object {
        fun fromDays(days: Int?): DateRangePreset {
            return entries.find { it.days == days } ?: SEVEN_DAYS
        }
    }

    /**
     * Convert this preset to a start timestamp (null for ALL_TIME)
     * Returns the timestamp for the start of the day X days ago
     */
    fun toStartTimestamp(): Long? {
        return when {
            this == ALL_TIME -> null
            this == CUSTOM -> null // Custom should use customStartDate
            days != null && days > 0 -> {
                val now = System.currentTimeMillis()
                now - (days * 24 * 60 * 60 * 1000L)
            }
            else -> null
        }
    }

    /**
     * Convert this preset to an end timestamp (null for ALL_TIME or no end date)
     */
    fun toEndTimestamp(): Long? {
        return when {
            this == ALL_TIME -> null
            this == CUSTOM -> null // Custom should use customEndDate
            days != null && days > 0 -> System.currentTimeMillis()
            else -> null
        }
    }
}

/**
 * Theme preferences
 */
enum class ThemePreference(val displayNameResId: Int) {
    SYSTEM(R.string.theme_preference_system),
    LIGHT(R.string.theme_preference_light),
    DARK(R.string.theme_preference_dark)
}

/**
 * Timestamp display format
 */
enum class TimestampFormat(val displayNameResId: Int) {
    RELATIVE(R.string.timestamp_format_relative),
    ABSOLUTE(R.string.timestamp_format_absolute)
}

/**
 * Favorite filter preset
 */
enum class FavoriteFilterPreset(val displayNameResId: Int) {
    ALL(R.string.favorite_filter_preset_all),
    FAVORITES_ONLY(R.string.favorite_filter_preset_favorites_only),
    NON_FAVORITES_ONLY(R.string.favorite_filter_preset_non_favorites_only);

    /**
     * Convert to Boolean? for the filter
     * null = show all, true = favorites only, false = non-favorites only
     */
    fun toFilterValue(): Boolean? {
        return when (this) {
            ALL -> null
            FAVORITES_ONLY -> true
            NON_FAVORITES_ONLY -> false
        }
    }

    /**
     * Convert to ActiveState for UI chips
     */
    fun toActiveState(): cut.the.crap.ui.components.ActiveState {
        return when (this) {
            ALL -> cut.the.crap.ui.components.ActiveState.Default
            FAVORITES_ONLY -> cut.the.crap.ui.components.ActiveState.Include
            NON_FAVORITES_ONLY -> cut.the.crap.ui.components.ActiveState.Exclude
        }
    }

    companion object {
        fun fromFilterValue(value: Boolean?): FavoriteFilterPreset {
            return when (value) {
                null -> ALL
                true -> FAVORITES_ONLY
                false -> NON_FAVORITES_ONLY
            }
        }
    }
}

/**
 * Sort order preset
 */
enum class SortOrderPreset(val displayNameResId: Int) {
    BY_ORDER(R.string.sort_order_preset_by_order),
    BY_DATE(R.string.sort_order_preset_by_date);

    /**
     * Returns true if sorting by date
     */
    fun isSortByDate(): Boolean {
        return this == BY_DATE
    }

    /**
     * Returns true if sorting by order/position
     */
    fun isSortByOrder(): Boolean {
        return this == BY_ORDER
    }
}

/**
 * How often automatic backups run on app start.
 * [intervalDays] is the minimum number of calendar days between automatic backups;
 * [OFF] disables automatic backups entirely.
 */
enum class BackupFrequency(val displayNameResId: Int, val intervalDays: Int) {
    OFF(R.string.backup_frequency_off, Int.MAX_VALUE),
    DAILY(R.string.backup_frequency_daily, 1),
    WEEKLY(R.string.backup_frequency_weekly, 7),
    MONTHLY(R.string.backup_frequency_monthly, 30)
}

/**
 * Retention policy applied after each successful backup.
 * [keepCount] is the number of most-recent backups to keep (null = keep all).
 */
enum class BackupRetention(val displayNameResId: Int, val keepCount: Int?) {
    KEEP_ALL(R.string.backup_retention_all, null),
    KEEP_5(R.string.backup_retention_5, 5),
    KEEP_10(R.string.backup_retention_10, 10),
    KEEP_30(R.string.backup_retention_30, 30)
}

/**
 * Settings data class
 */
data class AppSettings(
    // General
    val dateRangePreset: DateRangePreset = DateRangePreset.SEVEN_DAYS, // Deprecated - kept for migration
    val postsDateRangePreset: DateRangePreset = DateRangePreset.SEVEN_DAYS,
    val linksDateRangePreset: DateRangePreset = DateRangePreset.SEVEN_DAYS,
    val postsFavoriteFilterPreset: FavoriteFilterPreset = FavoriteFilterPreset.ALL,
    val linksFavoriteFilterPreset: FavoriteFilterPreset = FavoriteFilterPreset.ALL,
    val postsSortOrderPreset: SortOrderPreset = SortOrderPreset.BY_DATE,
    val linksSortOrderPreset: SortOrderPreset = SortOrderPreset.BY_DATE,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,

    // Display
    val timestampFormat: TimestampFormat = TimestampFormat.RELATIVE,
    val itemsPerLoad: Int = 50,

    // Content Filtering
    val showFavoritesOnly: Boolean = false, // Deprecated - kept for migration
    val autoHideOldItemsDays: Int? = null, // null = disabled

    // Database backups
    val backupFrequency: BackupFrequency = BackupFrequency.DAILY,
    val backupRetention: BackupRetention = BackupRetention.KEEP_ALL,

    // Sharing: when true, show an editable dialog before saving a shared link
    val editSharedLinkBeforeSave: Boolean = false,

    // Developer
    val developerMode: Boolean = false,
    val showPerformanceMetrics: Boolean = false,

    // X/Twitter API credentials (for resolving /i/status/ URLs)
    val xAuthToken: String? = null,
    val xCt0Token: String? = null
)
