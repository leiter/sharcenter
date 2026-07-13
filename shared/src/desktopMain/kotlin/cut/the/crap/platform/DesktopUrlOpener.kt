package cut.the.crap.platform

import java.awt.Desktop
import java.net.URI

/**
 * Opens URLs in the system browser.
 *
 * [preferApp] is ignored: a desktop has no equivalent of Android's package routing, and the URLs
 * the intent builders produce are plain web URLs that the browser handles correctly. The hint is
 * a preference, so ignoring it degrades cleanly rather than failing.
 */
class DesktopUrlOpener : UrlOpener {

    override fun open(url: String, preferApp: ExternalApp?) {
        try {
            val desktop = Desktop.getDesktop().takeIf {
                Desktop.isDesktopSupported() && it.isSupported(Desktop.Action.BROWSE)
            }
            if (desktop == null) {
                Log.w(TAG, "No browser available on this desktop; not opening $url")
                return
            }
            desktop.browse(URI(url))
        } catch (e: Exception) {
            Log.e(TAG, "Could not open: $url", e)
        }
    }

    private companion object {
        const val TAG = "DesktopUrlOpener"
    }
}
