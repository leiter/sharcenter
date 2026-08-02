package cut.the.crap.ui.content.links

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import cut.the.crap.ui.theme.PreviewAppThemeProvider
import cut.the.crap.ui.theme.PreviewThemeWrapper

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
private fun Preview(
    @PreviewParameter(PreviewAppThemeProvider::class) theme: PreviewThemeWrapper,
) {
    theme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // All items shown, no selection
            ListStatusBar(
                textInputExpanded = false,
                currentItemCount = 10,
                totalItemCount = 10,
                selectedItemCount = 0
            )

            // Filtered items, no selection
            ListStatusBar(
                textInputExpanded = false,
                currentItemCount = 5,
                totalItemCount = 10,
                selectedItemCount = 0
            )

            // Filtered items with selection
            ListStatusBar(
                textInputExpanded = false,
                currentItemCount = 5,
                totalItemCount = 10,
                selectedItemCount = 3
            )

            // All items with selection, text input expanded (no top padding)
            ListStatusBar(
                textInputExpanded = true,
                currentItemCount = 10,
                totalItemCount = 10,
                selectedItemCount = 7
            )
        }
    }
}
