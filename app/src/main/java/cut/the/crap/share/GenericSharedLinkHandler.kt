package cut.the.crap.share

import javax.inject.Inject

/**
 * Catch-all handler: saves any shared URL verbatim, with no resolution, handle extraction or
 * metadata enrichment. Must be registered last in [ShareModule] so platform-specific handlers get
 * first refusal on the URL.
 */
class GenericSharedLinkHandler @Inject constructor() : SharedLinkHandler {
    override fun recognizes(url: String): Boolean = true
}
