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
import org.jetbrains.compose.resources.stringResource
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.chip_from_prefix
import cut.the.crap.shared.resources.chip_to_prefix
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import cut.the.crap.tools.formatDateOnly
import cut.the.crap.ui.theme.conditional
import cut.the.crap.ui.theme.customClick
import cut.the.crap.ui.theme.textDependentHeight
import cut.the.crap.ui.theme.textDependentSize

// DateType and FilterState moved to FilterState.kt in :shared/commonMain (same package) so the
// ViewModels that reference them could move too. Only the rendering below stays Android-side.

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
                    DateType.START -> stringResource(Res.string.chip_from_prefix)
                    DateType.END -> stringResource(Res.string.chip_to_prefix)
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
