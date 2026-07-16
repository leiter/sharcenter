package cut.the.crap.platform

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** A message the iOS UI should surface (e.g. as a Compose snackbar). */
data class IosNotification(val message: String, val duration: NotificationDuration)

/**
 * iOS has no Toast, so — like the desktop actual — messages are published as a flow for the
 * Compose layer to render, and also echoed to stdout so nothing is silently swallowed before that
 * collector exists. [show] is non-suspending, so the buffer drops the oldest on overflow.
 */
class IosNotifier : Notifier {

    private val _notifications = MutableSharedFlow<IosNotification>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val notifications: SharedFlow<IosNotification> = _notifications.asSharedFlow()

    override fun show(message: String, duration: NotificationDuration) {
        println("[notify] $message")
        _notifications.tryEmit(IosNotification(message, duration))
    }
}
