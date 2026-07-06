package cut.the.crap.share

import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.mastodon.MastodonRepository
import cut.the.crap.tools.LinkMetadata
import cut.the.crap.tools.isMastodonUrl
import cut.the.crap.tools.parseMastodonUrl
import javax.inject.Inject

/**
 * Handles Mastodon shares:
 * - a shared *profile* link is stored as a fully-qualified `@user@instance` handle in the keyword
 *   pool (like X and Bluesky);
 * - a shared *post* link is saved as-is, then enriched with the author, post text and a thumbnail
 *   fetched from the origin instance's public status API via [MastodonRepository].
 */
class MastodonSharedLinkHandler @Inject constructor(
    private val mastodonRepository: MastodonRepository
) : SharedLinkHandler {

    override fun recognizes(url: String): Boolean = isMastodonUrl(url)

    override fun handleToSaveInstead(url: String): String? = mastodonProfileHandle(url)

    override suspend fun enrich(contentLink: ContentLink): ContentLink? {
        val url = contentLink.link
        if (!isMastodonUrl(url)) return null

        return when (val result = mastodonRepository.getPostMetadata(url)) {
            is Result.Success -> LinkMetadata.setMastodonMetadata(
                contentLink,
                authorName = result.data.authorName,
                postText = result.data.text,
                thumbnailUrl = result.data.thumbnailUrl.orEmpty(),
                contentType = "post"
            )
            is Result.Error -> null
        }
    }
}

/**
 * Pure helper: for a Mastodon *profile* URL returns the fully-qualified `"@user@instance"` handle to
 * store in the handle pool. A profile URL whose user segment is already federated (`user@remote`) is
 * kept as-is; a local one is qualified with the instance host so the handle is unambiguous across
 * the fediverse. Returns null for post links and non-Mastodon URLs, so those are saved as links.
 */
internal fun mastodonProfileHandle(url: String): String? {
    val info = parseMastodonUrl(url) ?: return null
    if (info.contentType != "profile") return null
    val user = info.username?.takeIf { it.isNotBlank() } ?: return null
    if (user.contains("@")) return "@$user"
    val host = info.additionalInfo["host"]?.substringAfter("://")?.takeIf { it.isNotBlank() }
        ?: return "@$user"
    return "@$user@$host"
}
