package cut.the.crap.platform

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

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

/** A message published by an [ObservableNotifier], for the UI to render. */
data class UserNotification(val message: String, val duration: NotificationDuration)

/**
 * A [Notifier] whose messages the shared UI must render itself.
 *
 * Android has a system Toast that floats above the app, so `AndroidNotifier` is a plain [Notifier]
 * and nothing else is needed. Every other target has no such facility, so the message has to become
 * a snackbar inside our own composition — which only works if something *collects* [notifications].
 * `App()` does that for whichever binding is observable; a platform that forgets to implement this
 * interface silently drops its user-facing messages, which is exactly the bug this exists to
 * prevent.
 */
interface ObservableNotifier : Notifier {
    val notifications: SharedFlow<UserNotification>
}

/**
 * The shared [ObservableNotifier] implementation — used verbatim by iOS and desktop.
 *
 * [show] is deliberately non-suspending to satisfy [Notifier], so the buffer drops the oldest
 * message rather than blocking when nothing is collecting yet (during startup, before the first
 * composition). Messages are also echoed to stdout: a dropped user-facing error is worse than a
 * noisy console, and the echo is what made this bug diagnosable in the first place.
 */
open class FlowNotifier : ObservableNotifier {

    private val _notifications = MutableSharedFlow<UserNotification>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val notifications: SharedFlow<UserNotification> = _notifications.asSharedFlow()

    override fun show(message: String, duration: NotificationDuration) {
        println("[notify] $message")
        _notifications.tryEmit(UserNotification(message, duration))
    }
}
