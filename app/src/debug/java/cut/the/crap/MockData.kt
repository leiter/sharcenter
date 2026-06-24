package cut.the.crap

import androidx.compose.ui.text.TextRange
import cut.the.crap.data.domain.ContentItem
import cut.the.crap.data.domain.ContentLink

data class TextFieldData(
    var text: String = "Hello there hope you are fine. :-) https://stackoverflow.com/questions/69849631/auto-mirroring-in-jetpack-compose-icons\nhttps://github.com/RajaNimit27/JetpackCompose_Ktor_Koin/tree/main",
    var selection: TextRange = TextRange(0, 0),
    var links: MutableList<String> = mutableListOf("https://stackoverflow.com/questions/69849631/auto-mirroring-in-jetpack-compose-icons", "https://github.com/RajaNimit27/JetpackCompose_Ktor_Koin/tree/main"),
    var charCounter: Int = 0,
    var highlightedLinkIndex: Int? = 0,
)


// Sample data for ContentLink previews
val mockedLinkItems = listOf(
    ContentLink(
        link = "http://www.cutthecrap.link/tweetjunky/status/178594766728348276754?t=24696734",
        added = System.currentTimeMillis(),
        description = "What I think is important about that...",
        favourite = true,
        hideItem = false
    ),
    ContentLink(
        id = 1,
        link = "https://example.com/article1",
        description = "Interesting article about Compose",
        added = System.currentTimeMillis(),
        favourite = true,
        hideItem = false
    ),
    ContentLink(
        id = 2,
        link = "https://example.com/article2",
        description = "Another cool link",
        added = System.currentTimeMillis() - 86400000,
        favourite = false,
        hideItem = false
    ),
    ContentLink(
        id = 3,
        link = "https://example.com/article3",
        description = "Great tutorial on ViewModels",
        added = System.currentTimeMillis() - 172800000,
        favourite = true,
        hideItem = false
    )
)

// Sample data for ContentItem (posts) previews
val mockedPostItems = listOf(
    ContentItem(
        id = 1,
        text = "This is an active content item with some sample text",
        isActive = true,
        isFavorite = false,
        lastModified = System.currentTimeMillis(),
        category = null
    ),
    ContentItem(
        id = 2,
        text = "This is a regular content item that is marked as favorite",
        isActive = false,
        isFavorite = true,
        lastModified = System.currentTimeMillis() - 86400000,
        category = "Important"
    ),
    ContentItem(
        id = 3,
        text = "This is a very long content item with lots of text that will trigger the expand/collapse button. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris.",
        isActive = false,
        isFavorite = false,
        lastModified = System.currentTimeMillis() - 172800000,
        category = "Draft"
    ),
    ContentItem(
        id = 4,
        text = "Short post with category",
        isActive = false,
        isFavorite = true,
        lastModified = System.currentTimeMillis() - 259200000,
        category = "Social"
    ),
    ContentItem(
        id = 5,
        text = "",
        isActive = false,
        isFavorite = false,
        lastModified = System.currentTimeMillis(),
        category = null
    )
)
