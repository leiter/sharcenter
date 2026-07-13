package cut.the.crap.platform

/**
 * There is no WebView on a desktop JVM, so the cookie-scraping sign-in flow cannot run at all.
 *
 * Unlike [DesktopSharer], there is no useful degradation to offer here — a fake login is worse
 * than no login — so [launch] does nothing and the desktop UI is expected to hide the entry point
 * and steer the user to manual credential entry instead.
 */
class DesktopLoginFlow : LoginFlow {

    override val isSupported: Boolean = false

    override fun launch(reason: LoginReason) {
        Log.w(TAG, "No WebView on desktop; X sign-in is unavailable. Use manual credentials.")
    }

    private companion object {
        const val TAG = "DesktopLoginFlow"
    }
}
