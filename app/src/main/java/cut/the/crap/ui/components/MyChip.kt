package cut.the.crap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cut.the.crap.tools.formatDateOnly
import cut.the.crap.ui.theme.conditional
import cut.the.crap.ui.theme.customClick
import cut.the.crap.ui.theme.textDependentHeight
import cut.the.crap.ui.theme.textDependentSize

enum class ActiveState {
    Disabled, Default, Include, Exclude;

    fun click(): ActiveState {
        return when (this.ordinal) {
            Disabled.ordinal -> Disabled
            Default.ordinal -> Include
            Include.ordinal -> Exclude
            Exclude.ordinal -> Default
            else -> Default
        }
    }

    fun toBoolean(): Boolean? {
        return when (this.ordinal) {
            Disabled.ordinal -> null
            Default.ordinal -> null
            Include.ordinal -> true
            Exclude.ordinal -> false
            else -> null
        }
    }
}

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

@Composable
fun <T : FilterState> MyChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: T,
    onLeadingClicked: (() -> Unit)? = null,
) {
    val touchTargetSize = 48
    val fontScale = LocalDensity.current.fontScale
    val multiplier = if (fontScale < 1) 1f else fontScale

    Box(modifier = modifier
        .heightIn(touchTargetSize.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(modifier = Modifier.textDependentHeight(touchTargetSize.dp),
            contentAlignment = Alignment.CenterEnd) {
            Column(
                modifier = Modifier
                    .heightIn(touchTargetSize.dp)
                    .padding(vertical = paddingVertical.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier
                    .textDependentHeight(28.dp)
                    .clip(RoundedCornerShape(24.dp))  // Clip the ripple to chip bounds
                    .customClick(
                        onClick = onClick,
                        role = Role.Checkbox,
                        noRipple = false,  // Enable ripple effect for touch feedback
                        enabled = !state.isDisabled(),
                        clickLabel = state.defaultLabel
                    )
                    .conditional(
                        condition = state.isDisabled(),
                        Modifier.semantics { selected = state.isApplied() }
                    )
                    .background(color = backgroundColor(state as FilterState))
                    .conditional(
                        state.isApplied(), Modifier.padding(end = (paddingHorizontal * multiplier).dp),
                    ), contentAlignment = Alignment.Center
                ) {
                    ChipContent(filterState = state)
                }
            }
        }
        if (state.isApplied()) {
            Box(
                modifier = Modifier
                    .textDependentSize(touchTargetSize.dp, touchTargetSize.dp)
                    .conditional(
                        onLeadingClicked != null,
                        Modifier.customClick(
                            onClick = onLeadingClicked ?: {},
                            role = Role.Button,
                            clickLabel = state.defaultLabel
                        )
                    ),
                contentAlignment = Alignment.Center) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = when (state) {
                        is FilterState.TripleState -> when (state.activeState) {
                            ActiveState.Include -> state.iconPainterInclude
                            ActiveState.Exclude -> Icons.Filled.Block
                            else -> Icons.Default.Phone
                        }
                        is FilterState.DateState -> state.imageVector
                        is FilterState.SingleActionState -> Icons.Filled.Close  // X icon for selected handle/tag chips
                        else -> Icons.Default.Phone
                    },
                    tint = when (state) {
                        is FilterState.TripleState -> when (state.activeState) {
                            ActiveState.Include -> MaterialTheme.colorScheme.onSecondaryContainer
                            ActiveState.Exclude -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        is FilterState.DateState -> MaterialTheme.colorScheme.onSurface
                        is FilterState.SingleActionState -> MaterialTheme.colorScheme.onSurface  // Icon color for SingleActionState
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun ChipContent(filterState: FilterState){
    val touchTargetSize = 48
    val fontScale = LocalDensity.current.fontScale
    val multiplier = if (fontScale < 1) 1f else fontScale
    when(filterState){
        is FilterState.TripleState -> Text(
            text = filterState.defaultLabel,
            modifier = Modifier
                .conditional(
                    filterState.isApplied(),
                    Modifier.padding(start = (touchTargetSize * multiplier).dp),
                    Modifier.padding(horizontal = (paddingHorizontal * multiplier).dp)
                ),
            maxLines = 1,
            textDecoration = if (filterState.activeState == ActiveState.Exclude)
                TextDecoration.LineThrough else null
            //color = textColor(state = state),
            //style = MyAppTheme.base.typography.bodyBold
        )

        is FilterState.DateState -> {
            val displayText = if (filterState.date != null) {
                val prefix = when (filterState.dateType) {
                    DateType.START -> "From: "
                    DateType.END -> "To: "
                }
                prefix + filterState.formattedDate
            } else {
                filterState.defaultLabel
            }

            Text(
                text = displayText,
                modifier = Modifier
                    .conditional(
                        filterState.isApplied(),
                        Modifier.padding(start = (touchTargetSize * multiplier).dp),
                        Modifier.padding(horizontal = (paddingHorizontal * multiplier).dp)
                    ),
                maxLines = 1,
                //color = textColor(state = state),
                //style = MyAppTheme.base.typography.bodyBold
            )
        }

        is FilterState.SingleActionState -> {
            Text(
                text = filterState.defaultLabel,
                modifier = Modifier
                    .conditional(
                        filterState.isApplied(),
                        Modifier.padding(start = (touchTargetSize * multiplier).dp),
                        Modifier.padding(horizontal = (paddingHorizontal * multiplier).dp)
                    ),
                maxLines = 1,
                //color = textColor(state = state),
                //style = MyAppTheme.base.typography.bodyBold
            )
        }
    }
}
@Composable
private fun backgroundColor(filterState: FilterState): Color {
    return when (filterState) {
        is FilterState.TripleState -> when (filterState.activeState) {
            ActiveState.Default -> MaterialTheme.colorScheme.surfaceVariant
            ActiveState.Disabled -> MaterialTheme.colorScheme.surfaceDim
            ActiveState.Include -> MaterialTheme.colorScheme.secondaryContainer
            ActiveState.Exclude -> MaterialTheme.colorScheme.errorContainer
        }

        is FilterState.DateState -> {
            when {
                filterState.isDisabled() -> MaterialTheme.colorScheme.surfaceDim
                filterState.isApplied() -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        }

        else -> {
            when {
                filterState.isDisabled() -> MaterialTheme.colorScheme.surfaceDim
                filterState.isApplied() -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.surface
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    cut.the.crap.ui.theme.MyAppTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Default (All items):",
                style = MaterialTheme.typography.labelSmall
            )
            MyChip(
                onClick = {},
                state = FilterState.TripleState(
                    defaultLabel = "Favorites",
                    iconPainterInclude = Icons.Filled.Star,
                    iconPainterExclude = Icons.Outlined.StarOutline,
                    activeState = ActiveState.Default
                )
            )

            Text(
                text = "Include (Only favorites):",
                style = MaterialTheme.typography.labelSmall
            )
            MyChip(
                onClick = {},
                state = FilterState.TripleState(
                    defaultLabel = "Favorites",
                    iconPainterInclude = Icons.Filled.Star,
                    iconPainterExclude = Icons.Outlined.StarOutline,
                    activeState = ActiveState.Include
                )
            )

            Text("Exclude (No favorites):", style = MaterialTheme.typography.labelSmall)
            MyChip(
                onClick = {},
                state = FilterState.TripleState(
                    defaultLabel = "Favorites",
                    iconPainterInclude = Icons.Filled.Star,
                    iconPainterExclude = Icons.Outlined.StarOutline,
                    activeState = ActiveState.Exclude
                )
            )

            Text("Disabled:", style = MaterialTheme.typography.labelSmall)
            MyChip(
                onClick = {},
                state = FilterState.TripleState(
                    defaultLabel = "Favorites",
                    iconPainterInclude = Icons.Filled.Star,
                    iconPainterExclude = Icons.Outlined.StarOutline,
                    activeState = ActiveState.Disabled
                )
            )

            Text("Date Range (Not set):", style = MaterialTheme.typography.labelSmall)
            MyChip(
                onClick = {},
                state = FilterState.DateState(
                    defaultLabel = "Date Range",
                    date = null,
                    dateType = DateType.START
                )
            )

            Text("Date - Start (Set):", style = MaterialTheme.typography.labelSmall)
            MyChip(
                onClick = {},
                onLeadingClicked = {},
                state = FilterState.DateState(
                    defaultLabel = "Start Date",
                    date = 1704067200000L, // Jan 1, 2024
                    dateType = DateType.START
                )
            )

            Text("Date - End (Set):", style = MaterialTheme.typography.labelSmall)
            MyChip(
                onClick = {},
                onLeadingClicked = {},
                state = FilterState.DateState(
                    defaultLabel = "End Date",
                    date = 1735689600000L, // Jan 1, 2025
                    dateType = DateType.END
                )
            )
        }
    }
}
