package cut.the.crap.platform

/**
 * The system clipboard.
 *
 * Compose does expose `LocalClipboard` in common code, but only inside composition. The callers
 * here are action handlers, not composables, so they need a plain injectable interface.
 */
interface Clipboard {
    fun copy(text: String)

    /** The clipboard's current text, or null if it holds nothing readable as text. */
    fun paste(): String?
}
