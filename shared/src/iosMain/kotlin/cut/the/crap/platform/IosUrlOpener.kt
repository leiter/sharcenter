package cut.the.crap.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * Opens URLs via the system (Safari or a registered app). [preferApp] is ignored — iOS routes to a
 * native client automatically when one claims the URL's universal link, and the intent builders
 * produce ordinary web URLs that Safari handles otherwise.
 */
class IosUrlOpener : UrlOpener {

    override fun open(url: String, preferApp: ExternalApp?) {
        val nsUrl = NSURL(string = url) ?: run {
            Log.w(TAG, "Not a valid URL: $url")
            return
        }
        UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }

    private companion object {
        const val TAG = "IosUrlOpener"
    }
}
