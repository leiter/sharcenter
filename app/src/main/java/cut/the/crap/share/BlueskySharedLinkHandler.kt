package cut.the.crap.share

import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.bluesky.BlueskyRepository
import cut.the.crap.tools.LinkMetadata
import cut.the.crap.tools.isBlueskyUrl
import cut.the.crap.tools.parseBlueskyUrl
import javax.inject.Inject

/**
 * Handles Bluesky shares:
 * - a shared *profile* link is stored as an `@handle` in the keyword pool (like X);
 * - a shared *post* link is saved as-is, then enriched with the author, post text and a thumbnail
 *   fetched from the public AppView API via [BlueskyRepository].
 */
class BlueskySharedLinkHandler @Inject constructor(
    private val blueskyRepository: BlueskyRepository
) : SharedLinkHandler {

    override fun recognizes(url: String): Boolean = isBlueskyUrl(url)

    override fun handleToSaveInstead(url: String): String? = blueskyProfileHandle(url)

    override suspend fun enrich(contentLink: ContentLink): ContentLink? {
        val url = contentLink.link
        if (!isBlueskyUrl(url)) return null

        return when (val result = blueskyRepository.getPostMetadata(url)) {
            is Result.Success -> LinkMetadata.setBlueskyMetadata(
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
 * Pure helper: for a Bluesky *profile* URL whose actor is a handle (e.g. `alice.bsky.social` or a
 * custom domain) returns the normalised `"@handle"` to store in the handle pool. Returns null for
 * post links, DID-based profile URLs (which have no human-friendly handle to store), and non-Bluesky
 * URLs, so those are saved as links instead.
 */
internal fun blueskyProfileHandle(url: String): String? {
    val info = parseBlueskyUrl(url) ?: return null
    if (info.contentType != "profile") return null
    val actor = info.username?.takeIf { it.isNotBlank() } ?: return null
    if (actor.startsWith("did:")) return null
    return "@$actor"
}
