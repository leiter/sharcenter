package cut.the.crap.platform

/** How long a transient message stays on screen. Mirrors Toast's two durations. */
enum class NotificationDuration { Short, Long }

/**
 * Transient, non-blocking user message — a Toast on Android.
 *
 * An interface rather than `expect`/`actual` because the implementation needs platform *state*
 * (Android's needs a `Context`), which `expect` objects cannot carry. The binding is supplied by
 * DI, so `commonMain` UI code just asks for a [Notifier].
 */
interface Notifier {
    fun show(message: String, duration: NotificationDuration = NotificationDuration.Short)
}
