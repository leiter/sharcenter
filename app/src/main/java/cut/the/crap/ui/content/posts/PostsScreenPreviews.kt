package cut.the.crap.ui.content.posts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.navigation.compose.rememberNavController
import cut.the.crap.data.domain.ContentItem
import cut.the.crap.ui.theme.PreviewAppThemeProvider
import cut.the.crap.ui.theme.PreviewThemeWrapper
import kotlinx.coroutines.flow.MutableStateFlow

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
private fun Preview(
    @PreviewParameter(PreviewAppThemeProvider::class) theme: PreviewThemeWrapper,
) {
    theme {
        val contentItemsFlow = remember {
            MutableStateFlow(
                listOf(
                    ContentItem(
                        id = 1,
                        text = "First content item for posting",
                        isActive = true,
                        isFavorite = false,
                        lastModified = System.currentTimeMillis()
                    ),
                    ContentItem(
                        id = 2,
                        text = "Second content item",
                        isActive = false,
                        isFavorite = true,
                        lastModified = System.currentTimeMillis() - 86400000
                    ),
                    ContentItem(
                        id = 3,
                        text = "Third content item with longer text that demonstrates how the card handles multiple lines of content",
                        isActive = false,
                        isFavorite = false,
                        lastModified = System.currentTimeMillis() - 172800000
                    )
                )
            )
        }
        val screenStateFlow = remember {
            MutableStateFlow(PostsScreenState())
        }

        PostsScreen(
            action = {},
            navController = rememberNavController(),
            screenState = screenStateFlow,
            contentItems = contentItemsFlow,
            onContentItemsReordered = {}
        )
    }
}
