package cut.the.crap.platform

/**
 * An app a URL can be routed to in preference to the browser.
 *
 * This is a *hint*, not a requirement — see [UrlOpener.open].
 */
enum class ExternalApp {
    X,
    Facebook,
    Instagram,
}

/**
 * Handing a URL to whatever the platform thinks should open it.
 *
 * An interface rather than `expect`/`actual`: the Android implementation needs a `Context`, and
 * `expect` objects cannot carry state. Supplied by DI.
 */
interface UrlOpener {
    /**
     * Opens [url] externally.
     *
     * [preferApp] asks the platform to route the URL to that app's native client when it is
     * installed, falling back to the browser when it is not — the URLs the intent builders
     * produce are ordinary web URLs, so the fallback always works. Whether an app is installed
     * is a platform question, so it is answered platform-side and never crosses into common
     * code; a platform with no notion of app routing (desktop) simply ignores the hint.
     */
    fun open(url: String, preferApp: ExternalApp? = null)
}
