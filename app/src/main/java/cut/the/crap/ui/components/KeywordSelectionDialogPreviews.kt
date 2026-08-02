package cut.the.crap.ui.components

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import cut.the.crap.data.domain.KeyWord
import cut.the.crap.ui.components.api.ChipsType
import cut.the.crap.ui.theme.PreviewAppThemeProvider
import cut.the.crap.ui.theme.PreviewThemeWrapper

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
private fun Preview(
    @PreviewParameter(PreviewAppThemeProvider::class) theme: PreviewThemeWrapper,
) {
    theme {
        // Sample data for preview
        val sampleItems = listOf(
            KeyWord(
                id = 1,
                text = "@elonmusk",
                type = cut.the.crap.data.domain.KeywordType.ACCOUNT,
                isFavorite = true,
                usageCount = 42,
                lastUsed = System.currentTimeMillis()
            ),
            KeyWord(
                id = 2,
                text = "@openai",
                type = cut.the.crap.data.domain.KeywordType.ACCOUNT,
                isFavorite = true,
                usageCount = 28,
                lastUsed = System.currentTimeMillis() - 86400000
            ),
            KeyWord(
                id = 3,
                text = "@github",
                type = cut.the.crap.data.domain.KeywordType.ACCOUNT,
                isFavorite = false,
                usageCount = 15,
                lastUsed = System.currentTimeMillis() - 172800000
            ),
            KeyWord(
                id = 4,
                text = "@kotlinlang",
                type = cut.the.crap.data.domain.KeywordType.ACCOUNT,
                isFavorite = false,
                usageCount = 8,
                lastUsed = System.currentTimeMillis() - 259200000
            ),
            KeyWord(
                id = 5,
                text = "@androiddev",
                type = cut.the.crap.data.domain.KeywordType.ACCOUNT,
                isFavorite = false,
                usageCount = 5,
                lastUsed = System.currentTimeMillis() - 345600000
            ),
            KeyWord(
                id = 6,
                text = "@jetbrainscompose",
                type = cut.the.crap.data.domain.KeywordType.ACCOUNT,
                isFavorite = true,
                usageCount = 12,
                lastUsed = System.currentTimeMillis() - 432000000
            )
        )

        val selectedItems = setOf(1, 2) // Pre-select first two items
        KeywordSelectionDialog(
            type = ChipsType.Handle,
            items = sampleItems,
            selectedItems = selectedItems,
            onItemToggle = {},
            onConfirm = {},
            onDismiss = {},
            onAdd = {}
        )
    }
}
