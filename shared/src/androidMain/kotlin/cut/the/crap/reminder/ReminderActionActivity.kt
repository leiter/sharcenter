package cut.the.crap.reminder

import android.app.Activity
import android.os.Bundle
import cut.the.crap.platform.Clipboard
import cut.the.crap.platform.ExternalApp
import cut.the.crap.platform.ReminderNotifier
import cut.the.crap.platform.UrlOpener
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Target of a reminder notification's "Post on X" / "Post on Facebook" buttons. Draws nothing:
 * copies the post when asked to, opens the prepared URL (in the native app when installed),
 * dismisses the notification, and finishes.
 *
 * An activity rather than a `PendingIntent` straight to the URL because a notification action does
 * not dismiss its notification — only this hop can. An activity rather than a receiver because
 * Android 12+ forbids notification trampolines: a receiver may not start the X or Facebook app.
 */
class ReminderActionActivity : Activity(), KoinComponent {

    private val clipboard: Clipboard by inject()
    private val urlOpener: UrlOpener by inject()
    private val reminderNotifier: ReminderNotifier by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra(EXTRA_URL)
        if (url != null) {
            // Facebook's sharer does not reliably prefill the text — same workaround as the composer.
            intent.getStringExtra(EXTRA_COPY_TEXT)?.let(clipboard::copy)
            val app = intent.getStringExtra(EXTRA_APP)?.let { name ->
                ExternalApp.entries.firstOrNull { it.name == name }
            }
            urlOpener.open(url, app)
        }
        intent.getIntExtra(EXTRA_REMINDER_ID, NO_REMINDER).takeIf { it != NO_REMINDER }
            ?.let(reminderNotifier::cancel)
        finish()
    }

    companion object {
        const val EXTRA_URL = "cut.the.crap.reminder.URL"
        const val EXTRA_APP = "cut.the.crap.reminder.APP"
        const val EXTRA_COPY_TEXT = "cut.the.crap.reminder.COPY_TEXT"
        const val EXTRA_REMINDER_ID = "cut.the.crap.reminder.REMINDER_ID"
        private const val NO_REMINDER = -1
    }
}
