package cut.the.crap.data.rest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * YouTube oEmbed API response
 * See: https://oembed.com/ and https://www.youtube.com/oembed
 */
@Serializable
data class YouTubeOEmbedResponse(
    @SerialName("title")
    val title: String,

    @SerialName("author_name")
    val authorName: String,

    @SerialName("author_url")
    val authorUrl: String,

    @SerialName("type")
    val type: String,  // "video"

    @SerialName("height")
    val height: Int,

    @SerialName("width")
    val width: Int,

    @SerialName("version")
    val version: String,  // "1.0"

    @SerialName("provider_name")
    val providerName: String,  // "YouTube"

    @SerialName("provider_url")
    val providerUrl: String,  // "https://www.youtube.com/"

    @SerialName("thumbnail_height")
    val thumbnailHeight: Int,

    @SerialName("thumbnail_width")
    val thumbnailWidth: Int,

    @SerialName("thumbnail_url")
    val thumbnailUrl: String,

    @SerialName("html")
    val html: String  // Embed HTML code
)

/**
 * Simplified YouTube video metadata for app usage
 */
data class YouTubeVideoMetadata(
    val videoId: String,
    val title: String,
    val channelName: String,
    val channelUrl: String,
    val thumbnailUrl: String,
    val thumbnailWidth: Int,
    val thumbnailHeight: Int,
    val embedHtml: String
) {
    companion object {
        /**
         * Creates YouTubeVideoMetadata from oEmbed response
         */
        fun fromOEmbed(videoId: String, response: YouTubeOEmbedResponse): YouTubeVideoMetadata {
            return YouTubeVideoMetadata(
                videoId = videoId,
                title = response.title,
                channelName = response.authorName,
                channelUrl = response.authorUrl,
                thumbnailUrl = response.thumbnailUrl,
                thumbnailWidth = response.thumbnailWidth,
                thumbnailHeight = response.thumbnailHeight,
                embedHtml = response.html
            )
        }
    }
}
