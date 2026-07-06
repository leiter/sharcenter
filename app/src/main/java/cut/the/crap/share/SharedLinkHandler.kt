package cut.the.crap.share

import cut.the.crap.data.domain.ContentLink

/**
 * Recognizes and processes a URL shared into the app. One implementation exists per social
 * platform that needs special treatment (X redirect resolution, YouTube metadata, …), plus the
 * [GenericSharedLinkHandler] catch-all. [ShareModule] assembles them into an ordered list and the
 * first handler whose [recognizes] returns true owns the share — so the catch-all must stay last.
 *
 * To support a new platform, add a handler implementing this interface and register it in
 * [ShareModule] ahead of the generic fallback. Every processing hook has a no-op default, so a
 * handler only overrides the steps it actually needs.
 */
interface SharedLinkHandler {

    /** Whether this handler recognizes [url] and should own its processing. */
    fun recognizes(url: String): Boolean

    /**
     * Resolve a redirect/short URL to its canonical destination before it is saved. May perform
     * network I/O and request interactive auth (see [UrlResolution]). Default: unchanged.
     */
    suspend fun resolve(url: String): UrlResolution = UrlResolution.Resolved(url, changed = false)

    /**
     * If [url] denotes an account/profile whose `@handle` should be stored in the keyword pool
     * *instead of* saving the link itself, return that handle (e.g. `"@user"`); otherwise null.
     * Default: null (save the URL as a link).
     */
    fun handleToSaveInstead(url: String): String? = null

    /**
     * After [contentLink] has been saved, fetch and apply extra metadata (title, thumbnail, …),
     * returning the updated link to persist, or null if there is nothing to enrich. May perform
     * network I/O. Default: null.
     */
    suspend fun enrich(contentLink: ContentLink): ContentLink? = null
}

/** Outcome of [SharedLinkHandler.resolve]. */
sealed interface UrlResolution {
    /**
     * Resolution finished. [url] is the canonical link to save and [changed] reports whether it
     * differs from the input (drives the "resolved and saved" vs "saved" feedback).
     */
    data class Resolved(val url: String, val changed: Boolean) : UrlResolution

    /**
     * Interactive authentication is required before the URL can be resolved. [credentialsPresent]
     * reports whether stale credentials already existed, so the caller can distinguish an expired
     * session from a first-time setup when prompting.
     */
    data class AuthRequired(val url: String, val credentialsPresent: Boolean) : UrlResolution
}
