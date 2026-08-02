package cut.the.crap.platform

import platform.UIKit.UIPasteboard

/** The system clipboard, via the general [UIPasteboard]. */
class IosClipboard : Clipboard {

    override fun copy(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    override fun paste(): String? = UIPasteboard.generalPasteboard.string
}
