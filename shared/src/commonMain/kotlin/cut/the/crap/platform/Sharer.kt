package cut.the.crap.platform

/**
 * Handing text to the platform's "share with another app" mechanism.
 *
 * Unlike [UrlOpener] and [Clipboard], this has no honest equivalent everywhere: Android has a
 * share sheet, a desktop JVM does not. So the capability is declared rather than assumed —
 * callers check [isSupported] and hide the affordance when it is false, instead of offering a
 * button that silently does nothing.
 */
interface Sharer {
    /** Whether this platform can actually hand text to another app. */
    val isSupported: Boolean

    /**
     * Offers [text] to another app, showing a chooser titled [chooserTitle].
     *
     * The caller supplies the already-localised title: resolving strings stays in the UI layer,
     * consistent with how typed errors are localised.
     */
    fun shareText(text: String, chooserTitle: String)
}
