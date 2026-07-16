package cut.the.crap.ui.content.posts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import cut.the.crap.tools.TextValueWrapper
import cut.the.crap.ui.theme.PreviewAppThemeProvider
import cut.the.crap.ui.theme.PreviewThemeWrapper

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
private fun Preview(
    @PreviewParameter(PreviewAppThemeProvider::class) theme: PreviewThemeWrapper,
) {
    theme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preview with empty content
            ContentEditor(
                value = TextValueWrapper("", Pair(0, 0)),
                onValueChange = {}
            )

            // Preview with some text
            ContentEditor(
                value = TextValueWrapper("This is a sample content being edited", Pair(0, 0)),
                onValueChange = {}
            )

            // Preview with long text
            ContentEditor(
                value = TextValueWrapper(
                    "This is a longer content item that spans multiple lines. It demonstrates how the editor handles text wrapping and displays character count for longer content.",
                    Pair(0, 0)
                ),
                onValueChange = {}
            )
        }
    }
}
