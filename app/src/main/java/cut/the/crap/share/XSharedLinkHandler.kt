package cut.the.crap.share

import cut.the.crap.data.preferences.SettingsRepository
import cut.the.crap.tools.UrlResolver
import cut.the.crap.tools.parseXUrl
import kotlinx.coroutines.flow.first

/**
 * Handles X/Twitter shares:
 * - resolves `/i/status/` redirect links to their canonical `x.com/{user}/status/{id}` form,
 *   using the signed-in user's credentials and prompting for login when they are missing/stale;
 * - stores a shared *profile* link as an `@handle` in the keyword pool instead of as a link.
 */
class XSharedLinkHandler constructor(
    private val settingsRepository: SettingsRepository
) : SharedLinkHandler {

    override fun recognizes(url: String): Boolean =
        url.contains("x.com") || url.contains("twitter.com")

    override suspend fun resolve(url: String): UrlResolution {
        // Only /i/status/ share links need resolving; everything else is already canonical.
        if (!UrlResolver.isXRedirectUrl(url)) return UrlResolution.Resolved(url, changed = false)

        val settings = settingsRepository.settingsFlow.first()
        // Bound to locals: AppSettings now lives in :shared, and Kotlin will not smart-cast a
        // property declared in another module (it cannot prove the getter is stable).
        val authToken = settings.xAuthToken
        val ct0Token = settings.xCt0Token
        val credentials = if (authToken != null && ct0Token != null) {
            UrlResolver.XCredentials(authToken, ct0Token)
        } else {
            null
        }

        return when (val result = UrlResolver.resolveXUrlWithStatus(url, credentials)) {
            is UrlResolver.ResolveResult.Success ->
                UrlResolution.Resolved(result.url, changed = result.url != url)
            is UrlResolver.ResolveResult.AuthRequired ->
                UrlResolution.AuthRequired(url, credentialsPresent = credentials != null)
            // Non-auth failure: fall back to saving the unresolved link rather than blocking.
            is UrlResolver.ResolveResult.Failed ->
                UrlResolution.Resolved(url, changed = false)
        }
    }

    override fun handleToSaveInstead(url: String): String? = xProfileHandle(url)
}

/**
 * Pure helper: for an X/Twitter *profile* URL returns the normalised `"@username"` to store in the
 * handle pool; returns null for post links, the `/i/…` share form, and non-X URLs. Kept free of
 * dependencies so the profile-only rule can be unit tested directly.
 */
internal fun xProfileHandle(url: String): String? {
    val info = parseXUrl(url) ?: return null
    if (info.contentType != "profile") return null
    val username = info.username?.takeIf { it.isNotBlank() } ?: return null
    return "@$username"
}
