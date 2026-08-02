package cut.the.crap.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.vector.ImageVector
import cut.the.crap.tools.formatDateOnly

/**
 * The state a filter chip can be in.
 *
 * This is a model, not a widget — but it lived inside `MyChip.kt` next to the composable that
 * renders it, which meant the ViewModels could not move to `commonMain` without dragging a whole
 * Compose file (and its Android-only `@Preview`) along with them. Splitting it out costs nothing:
 * it is the same package, so no call site changed.
 *
 * `ImageVector` and `Icons` are Compose Multiplatform types and are perfectly at home here; only
 * the *rendering* is platform-shaped, and that stays behind in `MyChip.kt` until WP7.
 *
 * Note there is a second, unrelated `FilterState` — a data class in `cut.the.crap.data.db` —
 * which describes the *query* filters. This one describes a chip.
 */

enum class DateType {
    START, END
}

sealed interface FilterState {

    val defaultLabel: String

    fun isDisabled(): Boolean {
        return when (val that = this) {
            is TripleState -> that.activeState == ActiveState.Disabled
            else -> false
        }
    }

    fun isApplied(): Boolean {
        return when (val that = this) {
            is TripleState -> that.activeState.ordinal > 1
            is DateState -> that.date != null
            is SingleActionState -> that.chosen
        }
    }

    data class TripleState(
        override val defaultLabel: String,
        val activeState: ActiveState,
        val iconPainterInclude: ImageVector,
        val iconPainterExclude: ImageVector,
    ) : FilterState {
        val painter: ImageVector
            get() = if (activeState == ActiveState.Include) iconPainterInclude
            else iconPainterExclude
    }

    data class SingleActionState(
        override val defaultLabel: String,
        val chosen: Boolean,
    ) : FilterState

    data class DateState(
        override val defaultLabel: String,
        val date: Long? = null,
        val dateType: DateType,
        val imageVector: ImageVector = Icons.Filled.Close,
    ) : FilterState {
        val formattedDate: String
            get() = date?.let {
                formatDateOnly(it)
            } ?: ""
    }
}
