package cut.the.crap.platform

/**
 * X sign-in is unavailable on iOS v1: like the desktop actual, the capability is declared false
 * rather than faked. A WKWebView-based flow could be added later; until then the UI hides the
 * entry point and steers the user to manual credential entry.
 */
class IosLoginFlow : LoginFlow {

    override val isSupported: Boolean = false

    override fun launch(reason: LoginReason) {
        Log.w(TAG, "WebView X sign-in is not available on iOS yet; use manual credentials.")
    }

    private companion object {
        const val TAG = "IosLoginFlow"
    }
}
