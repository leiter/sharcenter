package cut.the.crap.ui.content.posts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import cut.the.crap.data.domain.ContentItem
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
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Preview for active item
            ContentItemCard(
                contentItem = ContentItem(
                    id = 1,
                    text = "This is an active content item with some sample text",
                    isActive = true,
                    isFavorite = false,
                    lastModified = System.currentTimeMillis()
                ),
                action = {},
                isDragging = false
            )

            // Preview for inactive item with favorite
            ContentItemCard(
                contentItem = ContentItem(
                    id = 2,
                    text = "This is a regular content item that is marked as favorite",
                    isActive = false,
                    isFavorite = true,
                    lastModified = System.currentTimeMillis() - 86400000
                ),
                action = {},
                isDragging = false
            )

            // Preview for dragging item
            ContentItemCard(
                contentItem = ContentItem(
                    id = 3,
                    text = "This item is being dragged. Notice the visual styling indicating drag state.",
                    isActive = false,
                    isFavorite = false,
                    lastModified = System.currentTimeMillis()
                ),
                action = {},
                isDragging = true
            )

            // Preview for long text item (expandable)
            ContentItemCard(
                contentItem = ContentItem(
                    id = 4,
                    text = "This is a very long content item with lots of text that will trigger the expand/collapse button. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                    isActive = false,
                    isFavorite = false,
                    lastModified = System.currentTimeMillis(),
                    category = "Important"
                ),
                action = {},
                isDragging = false
            )

            // Preview for empty item
            ContentItemCard(
                contentItem = ContentItem(
                    id = 5,
                    text = "",
                    isActive = false,
                    isFavorite = false,
                    lastModified = System.currentTimeMillis()
                ),
                action = {},
                isDragging = false
            )
        }
    }
}
