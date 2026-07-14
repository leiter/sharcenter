package cut.the.crap.data.rest.tiktok

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Partial model for the public TikTok oEmbed response (`https://www.tiktok.com/oembed`), which needs
 * no authentication. Only the fields the app uses are declared; the Ktor client is configured with
 * `ignoreUnknownKeys`, so every other field is dropped safely.
 */
@Serializable
data class TikTokOEmbedResponse(
    val type: String? = null,                 // "video" for posts, "rich" for profiles
    val title: String? = null,                // the post caption
    @SerialName("author_name")
    val authorName: String? = null,           // creator display name
    @SerialName("author_url")
    val authorUrl: String? = null,
    @SerialName("author_unique_id")
    val authorUniqueId: String? = null,       // the @handle username
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null
)

/**
 * Simplified TikTok post metadata for app usage. [text] is the caption and [thumbnailUrl] is the
 * post's cover image.
 */
data class TikTokPostMetadata(
    val authorName: String,
    val authorHandle: String,
    val text: String,
    val thumbnailUrl: String?
) {
    companion object {
        /**
         * Maps an oEmbed response to [TikTokPostMetadata], or null when it carries no usable
         * content (e.g. an error payload returned with a 2xx status).
         */
        fun fromOEmbed(response: TikTokOEmbedResponse): TikTokPostMetadata? {
            val handle = response.authorUniqueId?.takeIf { it.isNotBlank() }
                ?: response.authorName?.takeIf { it.isNotBlank() }
                ?: return null
            val name = response.authorName?.takeIf { it.isNotBlank() } ?: handle
            return TikTokPostMetadata(
                authorName = name,
                authorHandle = handle,
                text = response.title.orEmpty(),
                thumbnailUrl = response.thumbnailUrl?.takeIf { it.isNotBlank() }
            )
        }
    }
}
