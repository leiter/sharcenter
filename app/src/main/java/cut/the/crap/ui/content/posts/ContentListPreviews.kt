package cut.the.crap.ui.content.posts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import cut.the.crap.ui.theme.PreviewAppThemeProvider
import cut.the.crap.ui.theme.PreviewThemeWrapper
import kotlinx.coroutines.flow.MutableStateFlow

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
private fun Preview(
    @PreviewParameter(PreviewAppThemeProvider::class) theme: PreviewThemeWrapper,
) {
    theme {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        ) {
            // Preview with multiple content items showing drag capabilities
            ContentList(
                action = {},
                paddingValues = PaddingValues(0.dp),
                contentItems = MutableStateFlow(emptyList()),  // mockedPostItems.take(3)
                onContentItemsReordered = {},
            )
        }
    }
}
