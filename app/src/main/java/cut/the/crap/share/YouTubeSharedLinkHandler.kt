package cut.the.crap.share

import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.YouTubeRepository
import cut.the.crap.data.rest.YouTubeUrlParser
import cut.the.crap.tools.LinkMetadata
import cut.the.crap.tools.parseSocialMediaUrl
import javax.inject.Inject

/**
 * Handles YouTube shares: the link is saved as-is, then enriched with the video's title, channel
 * name and thumbnail fetched from the oEmbed endpoint via [YouTubeRepository].
 */
class YouTubeSharedLinkHandler @Inject constructor(
    private val youTubeRepository: YouTubeRepository
) : SharedLinkHandler {

    override fun recognizes(url: String): Boolean = YouTubeUrlParser.isYouTubeUrl(url)

    override suspend fun enrich(contentLink: ContentLink): ContentLink? {
        val url = contentLink.link
        if (!YouTubeUrlParser.isYouTubeUrl(url)) return null

        return when (val result = youTubeRepository.getVideoMetadata(url)) {
            is Result.Success -> {
                val type = parseSocialMediaUrl(url)?.contentType ?: "video"
                LinkMetadata.setYouTubeMetadata(
                    contentLink,
                    channelName = result.data.channelName,
                    videoTitle = result.data.title,
                    thumbnailUrl = result.data.thumbnailUrl,
                    contentType = type
                )
            }
            is Result.Error -> null
        }
    }
}
