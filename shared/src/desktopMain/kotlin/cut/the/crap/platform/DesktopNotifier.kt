package cut.the.crap.platform

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** A message the desktop UI should surface. */
data class Notification(val message: String, val duration: NotificationDuration)

/**
 * Desktop has no Toast, so messages are published as a flow for the Compose window to render in
 * a snackbar (wired up in WP8). Until that window exists, they also go to stdout so nothing is
 * silently swallowed — a dropped user-facing error is worse than a noisy console.
 *
 * [show] is deliberately non-suspending to satisfy [Notifier], so the buffer drops the oldest
 * message rather than blocking if nothing is collecting yet.
 */
class DesktopNotifier : Notifier {

    private val _notifications = MutableSharedFlow<Notification>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val notifications: SharedFlow<Notification> = _notifications.asSharedFlow()

    override fun show(message: String, duration: NotificationDuration) {
        println("[notify] $message")
        _notifications.tryEmit(Notification(message, duration))
    }
}
