package cut.the.crap.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

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
