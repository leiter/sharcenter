package cut.the.crap.fake

import cut.the.crap.ui.content.settings.AppSettings
import cut.the.crap.ui.content.settings.DateRangePreset
import cut.the.crap.ui.content.settings.FavoriteFilterPreset
import cut.the.crap.ui.content.settings.SortOrderPreset
import cut.the.crap.ui.content.settings.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Fake implementation of SettingsRepository for testing.
 * Mimics the DataStore-based SettingsRepository with in-memory storage.
 */
class FakeSettingsRepository {

    private val _settingsFlow = MutableStateFlow(AppSettings())
    val settingsFlow: Flow<AppSettings> = _settingsFlow

    suspend fun updateSettings(settings: AppSettings) {
        _settingsFlow.value = settings
    }

    suspend fun updateDateRangePreset(preset: DateRangePreset) {
        _settingsFlow.update {
            it.copy(
                postsDateRangePreset = preset,
                linksDateRangePreset = preset
            )
        }
    }

    suspend fun updatePostsDateRangePreset(preset: DateRangePreset) {
        _settingsFlow.update { it.copy(postsDateRangePreset = preset) }
    }

    suspend fun updateLinksDateRangePreset(preset: DateRangePreset) {
        _settingsFlow.update { it.copy(linksDateRangePreset = preset) }
    }

    suspend fun updatePostsFavoriteFilterPreset(preset: FavoriteFilterPreset) {
        _settingsFlow.update { it.copy(postsFavoriteFilterPreset = preset) }
    }

    suspend fun updateLinksFavoriteFilterPreset(preset: FavoriteFilterPreset) {
        _settingsFlow.update { it.copy(linksFavoriteFilterPreset = preset) }
    }

    suspend fun updatePostsSortOrderPreset(preset: SortOrderPreset) {
        _settingsFlow.update { it.copy(postsSortOrderPreset = preset) }
    }

    suspend fun updateLinksSortOrderPreset(preset: SortOrderPreset) {
        _settingsFlow.update { it.copy(linksSortOrderPreset = preset) }
    }

    suspend fun updateThemePreference(theme: ThemePreference) {
        _settingsFlow.update { it.copy(themePreference = theme) }
    }

    suspend fun updateDeveloperMode(enabled: Boolean) {
        _settingsFlow.update { it.copy(developerMode = enabled) }
    }

    suspend fun updateXCredentials(authToken: String?, ct0Token: String?) {
        _settingsFlow.update {
            it.copy(xAuthToken = authToken, xCt0Token = ct0Token)
        }
    }

    // Test helpers
    fun setSettings(settings: AppSettings) {
        _settingsFlow.value = settings
    }

    fun getCurrentSettings(): AppSettings = _settingsFlow.value

    fun reset() {
        _settingsFlow.value = AppSettings()
    }
}
