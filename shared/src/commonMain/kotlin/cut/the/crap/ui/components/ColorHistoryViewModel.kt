package cut.the.crap.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cut.the.crap.data.preferences.ColorHistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Exposes the recently picked colours (as RRGGBB hex strings, most-recent first) and records new
 * picks. Any screen that shows a [ColorPicker]/[ColorPickerDialog] can obtain this via
 * `koinViewModel()` to give the picker a shared, persistent history strip.
 */
class ColorHistoryViewModel constructor(
    private val colorHistoryRepository: ColorHistoryRepository
) : ViewModel() {

    val recentColors: StateFlow<List<String>> = colorHistoryRepository.recentColors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Record [hex] (RRGGBB, with or without a leading '#') as the most recently picked colour. */
    fun recordColor(hex: String) {
        viewModelScope.launch { colorHistoryRepository.recordColor(hex) }
    }
}
