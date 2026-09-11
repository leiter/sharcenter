package cut.the.crap.platform

/**
 * Keeps the background job that fires action reminders scheduled — WorkManager on Android.
 *
 * Declared rather than assumed, like [Sharer]: a platform without background execution reports
 * [isSupported] = false and the reminder screen's entry point is hidden, instead of offering a
 * feature that silently never fires. An interface rather than `expect`/`actual` because the
 * Android implementation needs a `Context`.
 */
interface ReminderScheduler {
    val isSupported: Boolean

    /** Idempotent: schedules the job if it is not scheduled yet, otherwise leaves it alone. */
    fun ensureScheduled()

    /** Stops the job — called when no enabled reminder remains. */
    fun cancel()
}

/**
 * What a reminder notification shows and where its buttons lead.
 *
 * The URLs are built in common code from the existing intent builders, so the platform only wraps
 * them; a null URL means the reminder does not target that platform and gets no button for it.
 *
 * @property postLabel The campaign's variant label (e.g. "DE-2").
 * @property facebookUrl Facebook's sharer does not reliably prefill [text], so the platform copies
 *   [text] to the clipboard before opening it — the same workaround the composer uses.
 */
data class ReminderNotification(
    val reminderId: Int,
    val countryCode: String,
    val postLabel: String,
    val text: String,
    val xUrl: String?,
    val facebookUrl: String?,
)

/** Posts reminder notifications — system notifications on Android. */
interface ReminderNotifier {
    /** True when a notification would actually be seen: permission granted, channel not blocked. */
    val canNotify: Boolean

    suspend fun show(notification: ReminderNotification)

    fun cancel(reminderId: Int)

    /** Opens the platform's notification settings for this app, for when the permission is denied. */
    fun openSettings()
}

/** The [ReminderScheduler] for platforms without reminder support (desktop, iOS for now). */
object UnsupportedReminderScheduler : ReminderScheduler {
    override val isSupported: Boolean = false
    override fun ensureScheduled() = Unit
    override fun cancel() = Unit
}

/** The [ReminderNotifier] for platforms without reminder support (desktop, iOS for now). */
object UnsupportedReminderNotifier : ReminderNotifier {
    override val canNotify: Boolean = false
    override suspend fun show(notification: ReminderNotification) = Unit
    override fun cancel(reminderId: Int) = Unit
    override fun openSettings() = Unit
}
