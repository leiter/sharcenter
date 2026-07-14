package cut.the.crap.share

import cut.the.crap.data.domain.ContentLink
import cut.the.crap.data.rest.Result
import cut.the.crap.data.rest.reddit.RedditRepository
import cut.the.crap.tools.LinkMetadata
import cut.the.crap.tools.UrlResolver
import cut.the.crap.tools.isRedditShortLink
import cut.the.crap.tools.isRedditUrl
import cut.the.crap.tools.parseRedditUrl

/**
 * Handles Reddit shares:
 * - a `redd.it/{id}` or `/s/` short link is first expanded to its canonical `/comments/{id}/` form
 *   by following the redirect, so it can be parsed and enriched;
 * - a shared *user* link is stored as a `u/username` handle and a *subreddit* link as an
 *   `r/subreddit` handle in the keyword pool;
 * - a shared *post* link is saved as-is, then enriched with the author and title fetched from the
 *   public oEmbed endpoint via [RedditRepository] (Reddit's oEmbed returns no thumbnail).
 */
class RedditSharedLinkHandler constructor(
    private val redditRepository: RedditRepository,
    private val urlResolver: UrlResolver,
) : SharedLinkHandler {

    override fun recognizes(url: String): Boolean = isRedditUrl(url)

    override suspend fun resolve(url: String): UrlResolution {
        // Canonical URLs are already enrichable; only short/redirect links need expanding.
        if (!isRedditShortLink(url)) return UrlResolution.Resolved(url, changed = false)
        val resolved = urlResolver.resolveRedirect(url)
        return UrlResolution.Resolved(resolved, changed = resolved != url)
    }

    override fun handleToSaveInstead(url: String): String? = redditHandle(url)

    override suspend fun enrich(contentLink: ContentLink): ContentLink? {
        val url = contentLink.link
        if (!isRedditUrl(url)) return null

        return when (val result = redditRepository.getPostMetadata(url)) {
            is Result.Success -> LinkMetadata.setRedditMetadata(
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
 * Pure helper: for a Reddit *user* profile returns the `"u/username"` handle and for a *subreddit*
 * the `"r/subreddit"` handle to store in the keyword pool — Reddit's native mention forms. Returns
 * null for post links, short links and non-Reddit URLs, so those are saved as links instead.
 */
internal fun redditHandle(url: String): String? {
    val info = parseRedditUrl(url) ?: return null
    val name = info.username?.takeIf { it.isNotBlank() } ?: return null
    return when (info.contentType) {
        "profile" -> "u/$name"
        "subreddit" -> "r/$name"
        else -> null
    }
}
