package cut.the.crap.ui.content.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cut.the.crap.data.preferences.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // Expose settings as StateFlow
    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    // Update settings
    fun updateSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(newSettings)
        }
    }

    // Convenience methods for individual updates
    fun updateDateRangePreset(preset: DateRangePreset) {
        viewModelScope.launch {
            settingsRepository.updateDateRangePreset(preset)
        }
    }

    fun updatePostsDateRangePreset(preset: DateRangePreset) {
        viewModelScope.launch {
            settingsRepository.updatePostsDateRangePreset(preset)
        }
    }

    fun updateLinksDateRangePreset(preset: DateRangePreset) {
        viewModelScope.launch {
            settingsRepository.updateLinksDateRangePreset(preset)
        }
    }

    fun updatePostsFavoriteFilterPreset(preset: FavoriteFilterPreset) {
        viewModelScope.launch {
            settingsRepository.updatePostsFavoriteFilterPreset(preset)
        }
    }

    fun updateLinksFavoriteFilterPreset(preset: FavoriteFilterPreset) {
        viewModelScope.launch {
            settingsRepository.updateLinksFavoriteFilterPreset(preset)
        }
    }

    fun updatePostsSortOrderPreset(preset: SortOrderPreset) {
        viewModelScope.launch {
            settingsRepository.updatePostsSortOrderPreset(preset)
        }
    }

    fun updateLinksSortOrderPreset(preset: SortOrderPreset) {
        viewModelScope.launch {
            settingsRepository.updateLinksSortOrderPreset(preset)
        }
    }

    fun updateThemePreference(theme: ThemePreference) {
        viewModelScope.launch {
            settingsRepository.updateThemePreference(theme)
        }
    }

    fun toggleDeveloperMode() {
        viewModelScope.launch {
            val currentSettings = settings.value
            settingsRepository.updateDeveloperMode(!currentSettings.developerMode)
        }
    }
}
