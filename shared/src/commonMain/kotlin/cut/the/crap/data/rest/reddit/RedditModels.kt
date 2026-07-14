package cut.the.crap.data.rest.reddit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Partial model for the public Reddit oEmbed response (`https://www.reddit.com/oembed`), which needs
 * no authentication. Reddit locked down its unauthenticated `.json` endpoints (403), so oEmbed is
 * the free path — it returns the title and author but, unlike YouTube/TikTok, no thumbnail image.
 * Only the fields the app uses are declared; the Ktor client's `ignoreUnknownKeys` drops the rest.
 */
@Serializable
data class RedditOEmbedResponse(
    val title: String? = null,                // the post title
    @SerialName("author_name")
    val authorName: String? = null,           // the poster's username (without the "u/" prefix)
    val type: String? = null,                 // "rich"
    @SerialName("provider_name")
    val providerName: String? = null,         // "reddit"
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null          // usually absent for Reddit
)

/**
 * Simplified Reddit post metadata for app usage. [authorName] is normalised to the `u/username`
 * form. [thumbnailUrl] is typically null because Reddit's oEmbed omits it.
 */
data class RedditPostMetadata(
    val authorName: String,
    val text: String,
    val thumbnailUrl: String?
) {
    companion object {
        /** Maps an oEmbed response to [RedditPostMetadata], or null when it carries no title. */
        fun fromOEmbed(response: RedditOEmbedResponse): RedditPostMetadata? {
            val title = response.title?.takeIf { it.isNotBlank() } ?: return null
            val author = response.authorName?.takeIf { it.isNotBlank() }
                ?.let { if (it.startsWith("u/")) it else "u/$it" }
                ?: ""
            return RedditPostMetadata(
                authorName = author,
                text = title,
                thumbnailUrl = response.thumbnailUrl?.takeIf { it.isNotBlank() }
            )
        }
    }
}
