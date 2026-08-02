package cut.the.crap.platform

import platform.UIKit.UIActivityViewController

/**
 * The iOS share sheet, via [UIActivityViewController]. Fully supported, so — unlike desktop — the
 * UI keeps the share affordance. [chooserTitle] has no direct analogue (iOS titles the sheet
 * itself), so it is ignored; the text is the shared item.
 */
class IosSharer : Sharer {

    override val isSupported: Boolean = true

    override fun shareText(text: String, chooserTitle: String) {
        val controller = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null,
        )
        topViewController()?.presentViewController(controller, animated = true, completion = null)
    }
}
