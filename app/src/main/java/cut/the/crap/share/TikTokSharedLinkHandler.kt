package cut.the.crap.share

import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.tiktok.TikTokRepository
import cut.the.crap.tools.LinkMetadata
import cut.the.crap.tools.UrlResolver
import cut.the.crap.tools.isTikTokShortLink
import cut.the.crap.tools.isTikTokUrl
import cut.the.crap.tools.parseTikTokUrl

/**
 * Handles TikTok shares:
 * - a `vm.`/`vt.tiktok.com` short link is first expanded to its canonical `/@user/video/{id}` form
 *   by following the redirect, so it can be parsed and enriched;
 * - a shared *profile* link is stored as an `@handle` in the keyword pool (like X and Bluesky);
 * - a shared *post* link is saved as-is, then enriched with the author, caption and cover thumbnail
 *   fetched from the public oEmbed endpoint via [TikTokRepository].
 */
class TikTokSharedLinkHandler constructor(
    private val tikTokRepository: TikTokRepository
) : SharedLinkHandler {

    override fun recognizes(url: String): Boolean = isTikTokUrl(url)

    override suspend fun resolve(url: String): UrlResolution {
        // Canonical URLs are already enrichable; only short/redirect links need expanding.
        if (!isTikTokShortLink(url)) return UrlResolution.Resolved(url, changed = false)
        val resolved = UrlResolver.resolveRedirect(url)
        return UrlResolution.Resolved(resolved, changed = resolved != url)
    }

    override fun handleToSaveInstead(url: String): String? = tikTokProfileHandle(url)

    override suspend fun enrich(contentLink: ContentLink): ContentLink? {
        val url = contentLink.link
        if (!isTikTokUrl(url)) return null

        return when (val result = tikTokRepository.getPostMetadata(url)) {
            is Result.Success -> LinkMetadata.setTikTokMetadata(
                contentLink,
                authorName = result.data.authorName,
                postText = result.data.text,
                thumbnailUrl = result.data.thumbnailUrl.orEmpty(),
                contentType = parseTikTokUrl(url)?.contentType ?: "video"
            )
            is Result.Error -> null
        }
    }
}

/**
 * Pure helper: for a TikTok *profile* URL returns the `"@user"` handle to store in the handle pool.
 * Returns null for post links (video/photo), short links and non-TikTok URLs, so those are saved as
 * links instead.
 */
internal fun tikTokProfileHandle(url: String): String? {
    val info = parseTikTokUrl(url) ?: return null
    if (info.contentType != "profile") return null
    val user = info.username?.takeIf { it.isNotBlank() } ?: return null
    return "@$user"
}
