package cut.the.crap.testutils

import cut.the.crap.data.domain.ContentItem
import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.domain.KeyWord
import cut.the.crap.data.domain.KeywordType
import cut.the.crap.data.rest.YouTubeVideoMetadata
import cut.the.crap.ui.content.settings.AppSettings
import cut.the.crap.ui.content.settings.DateRangePreset
import cut.the.crap.ui.content.settings.FavoriteFilterPreset
import cut.the.crap.ui.content.settings.SortOrderPreset
import cut.the.crap.ui.content.settings.ThemePreference

/**
 * Factory object for creating test data objects with sensible defaults.
 * Simplifies test setup by providing pre-configured test objects.
 */
object TestData {

    // Time constants for testing
    const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
    const val ONE_WEEK_MS = 7 * ONE_DAY_MS
    val NOW = System.currentTimeMillis()

    // ContentLink helpers
    fun contentLink(
        id: Int = 1,
        link: String = "https://x.com/user/status/123",
        description: String = "Test description",
        added: Long = NOW,
        position: Int = id,
        favourite: Boolean = false,
        hideItem: Boolean = false
    ) = ContentLink(
        id = id,
        link = link,
        description = description,
        added = added,
        position = position,
        favourite = favourite,
        hideItem = hideItem
    )

    fun contentLinks(count: Int, startId: Int = 1): List<ContentLink> {
        return (startId until startId + count).map { id ->
            contentLink(id = id, link = "https://x.com/user$id/status/$id")
        }
    }

    fun xLink(id: Int = 1, username: String = "testuser") = contentLink(
        id = id,
        link = "https://x.com/$username/status/$id",
        description = "X post by $username"
    )

    fun youtubeLink(id: Int = 1, videoId: String = "dQw4w9WgXcQ") = contentLink(
        id = id,
        link = "https://www.youtube.com/watch?v=$videoId",
        description = "YouTube video"
    )

    fun facebookLink(id: Int = 1) = contentLink(
        id = id,
        link = "https://fb.com/post/$id",
        description = "Facebook post"
    )

    // ContentItem helpers
    fun contentItem(
        id: Int = 1,
        text: String = "Test content text",
        created: Long = NOW,
        lastModified: Long = NOW,
        sortOrder: Int = 0,
        isFavorite: Boolean = false,
        category: String? = null,
        isActive: Boolean = false
    ) = ContentItem(
        id = id,
        text = text,
        created = created,
        lastModified = lastModified,
        sortOrder = sortOrder,
        isFavorite = isFavorite,
        category = category,
        isActive = isActive
    )

    fun contentItems(count: Int, startId: Int = 1): List<ContentItem> {
        return (startId until startId + count).map { id ->
            contentItem(id = id, text = "Content item #$id", sortOrder = id - startId)
        }
    }

    fun activeContentItem(id: Int = 1, text: String = "Active content") = contentItem(
        id = id,
        text = text,
        isActive = true
    )

    // KeyWord helpers
    fun keyWord(
        id: Int = 1,
        text: String = "@testhandle",
        type: KeywordType = KeywordType.ACCOUNT,
        created: Long = NOW,
        lastUsed: Long = NOW,
        usageCount: Int = 0,
        isFavorite: Boolean = false,
        category: String? = null,
        color: String? = null,
        isArchived: Boolean = false,
        sortOrder: Int = 0
    ) = KeyWord(
        id = id,
        text = text,
        type = type,
        created = created,
        lastUsed = lastUsed,
        usageCount = usageCount,
        isFavorite = isFavorite,
        category = category,
        color = color,
        isArchived = isArchived,
        sortOrder = sortOrder
    )

    fun account(id: Int = 1, text: String = "@user$id") = keyWord(
        id = id,
        text = text,
        type = KeywordType.ACCOUNT
    )

    fun hashtag(id: Int = 1, text: String = "#tag$id") = keyWord(
        id = id,
        text = text,
        type = KeywordType.HASHTAG
    )

    fun tag(id: Int = 1, text: String = "keyword$id") = keyWord(
        id = id,
        text = text,
        type = KeywordType.TAG
    )

    fun accounts(count: Int, startId: Int = 1): List<KeyWord> {
        return (startId until startId + count).map { id ->
            account(id = id, text = "@user$id")
        }
    }

    fun hashtags(count: Int, startId: Int = 1): List<KeyWord> {
        return (startId until startId + count).map { id ->
            hashtag(id = id, text = "#tag$id")
        }
    }

    fun tags(count: Int, startId: Int = 1): List<KeyWord> {
        return (startId until startId + count).map { id ->
            tag(id = id, text = "keyword$id")
        }
    }

    // AppSettings helpers
    fun appSettings(
        dateRangePreset: DateRangePreset = DateRangePreset.SEVEN_DAYS,
        postsDateRangePreset: DateRangePreset = DateRangePreset.SEVEN_DAYS,
        linksDateRangePreset: DateRangePreset = DateRangePreset.SEVEN_DAYS,
        postsFavoriteFilterPreset: FavoriteFilterPreset = FavoriteFilterPreset.ALL,
        linksFavoriteFilterPreset: FavoriteFilterPreset = FavoriteFilterPreset.ALL,
        postsSortOrderPreset: SortOrderPreset = SortOrderPreset.BY_ORDER,
        linksSortOrderPreset: SortOrderPreset = SortOrderPreset.BY_DATE,
        customStartDate: Long? = null,
        customEndDate: Long? = null,
        themePreference: ThemePreference = ThemePreference.SYSTEM,
        developerMode: Boolean = false
    ) = AppSettings(
        dateRangePreset = dateRangePreset,
        postsDateRangePreset = postsDateRangePreset,
        linksDateRangePreset = linksDateRangePreset,
        postsFavoriteFilterPreset = postsFavoriteFilterPreset,
        linksFavoriteFilterPreset = linksFavoriteFilterPreset,
        postsSortOrderPreset = postsSortOrderPreset,
        linksSortOrderPreset = linksSortOrderPreset,
        customStartDate = customStartDate,
        customEndDate = customEndDate,
        themePreference = themePreference,
        developerMode = developerMode
    )

    // YouTubeVideoMetadata helpers
    fun youtubeMetadata(
        videoId: String = "dQw4w9WgXcQ",
        title: String = "Test Video Title",
        channelName: String = "Test Channel",
        channelUrl: String = "https://www.youtube.com/channel/testchannel",
        thumbnailUrl: String = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
        thumbnailWidth: Int = 480,
        thumbnailHeight: Int = 360,
        embedHtml: String = "<iframe></iframe>"
    ) = YouTubeVideoMetadata(
        videoId = videoId,
        title = title,
        channelName = channelName,
        channelUrl = channelUrl,
        thumbnailUrl = thumbnailUrl,
        thumbnailWidth = thumbnailWidth,
        thumbnailHeight = thumbnailHeight,
        embedHtml = embedHtml
    )
}
