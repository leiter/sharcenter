package cut.the.crap.platform

/**
 * There is no share sheet on a desktop JVM, so [isSupported] is false and the desktop UI is
 * expected to hide the share affordance entirely.
 *
 * [shareText] still degrades usefully rather than throwing — it copies the text and says so —
 * because "supported" is a UI hint, not an enforced precondition, and a silently dead button
 * is worse than a clipboard copy.
 */
class DesktopSharer(
    private val clipboard: Clipboard,
    private val notifier: Notifier,
) : Sharer {

    override val isSupported: Boolean = false

    override fun shareText(text: String, chooserTitle: String) {
        clipboard.copy(text)
        notifier.show("Copied to clipboard — no share sheet on desktop.")
    }
}
