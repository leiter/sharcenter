package cut.the.crap.reminder

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cut.the.crap.platform.ExternalApp
import cut.the.crap.platform.Log
import cut.the.crap.platform.ReminderNotification
import cut.the.crap.platform.ReminderNotifier
import cut.the.crap.shared.R
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.reminder_action_post_facebook
import cut.the.crap.shared.resources.reminder_action_post_x
import cut.the.crap.shared.resources.reminder_channel_description
import cut.the.crap.shared.resources.reminder_channel_name
import cut.the.crap.shared.resources.reminder_notification_title
import org.jetbrains.compose.resources.getString

/**
 * System notifications for action reminders (doc/ACTION_REMINDER_SPEC.md §2.3).
 *
 * The body tap opens the app on the reminder screen; the X / Facebook buttons go through
 * [ReminderActionActivity], which opens the prepared post and dismisses the notification.
 * Notifications are tagged, so a reminder's id cannot collide with another feature's.
 */
class AndroidReminderNotifier(private val context: Context) : ReminderNotifier {

    private val manager = NotificationManagerCompat.from(context)

    /**
     * Covers the API 33+ runtime permission too: `areNotificationsEnabled()` is false until it is
     * granted. A channel the user switched off counts as blocked; one not created yet does not.
     */
    override val canNotify: Boolean
        get() = manager.areNotificationsEnabled() &&
            manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE

    // The permission is checked by the caller (ReminderDispatcher asks canNotify first); the
    // SecurityException catch covers it being revoked in between.
    @SuppressLint("MissingPermission")
    override suspend fun show(notification: ReminderNotification) {
        ensureChannel()
        val id = notification.reminderId
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(getString(Res.string.reminder_notification_title, notification.postLabel))
            .setContentText(notification.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
        openRemindersIntent(id)?.let(builder::setContentIntent)
        notification.xUrl?.let { url ->
            builder.addAction(
                0,
                getString(Res.string.reminder_action_post_x),
                actionIntent(id, SLOT_X, url, ExternalApp.X, copyText = null),
            )
        }
        notification.facebookUrl?.let { url ->
            builder.addAction(
                0,
                getString(Res.string.reminder_action_post_facebook),
                actionIntent(id, SLOT_FACEBOOK, url, ExternalApp.Facebook, copyText = notification.text),
            )
        }
        try {
            manager.notify(NOTIFICATION_TAG, id, builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission was revoked; reminder $id not shown.", e)
        }
    }

    override fun cancel(reminderId: Int) {
        manager.cancel(NOTIFICATION_TAG, reminderId)
    }

    override fun openSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            // The injected Context is the Application, which has no task to launch into.
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Notification settings could not be opened.", e)
        }
    }

    /** Re-created on every show so the channel name follows a changed device language. */
    private suspend fun ensureChannel() {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(getString(Res.string.reminder_channel_name))
            .setDescription(getString(Res.string.reminder_channel_description))
            .build()
        manager.createNotificationChannel(channel)
    }

    /**
     * The launcher intent plus the route to open. Resolved through the package manager because
     * `MainActivity` lives in `:app`, which this module cannot see.
     */
    private fun openRemindersIntent(reminderId: Int): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        launch.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_OPEN_ROUTE, ACTION_REMINDERS_ROUTE)
        return PendingIntent.getActivity(context, requestCode(reminderId, SLOT_BODY), launch, PENDING_FLAGS)
    }

    private fun actionIntent(
        reminderId: Int,
        slot: Int,
        url: String,
        app: ExternalApp,
        copyText: String?,
    ): PendingIntent {
        val intent = Intent(context, ReminderActionActivity::class.java)
            .putExtra(ReminderActionActivity.EXTRA_URL, url)
            .putExtra(ReminderActionActivity.EXTRA_APP, app.name)
            .putExtra(ReminderActionActivity.EXTRA_REMINDER_ID, reminderId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        copyText?.let { intent.putExtra(ReminderActionActivity.EXTRA_COPY_TEXT, it) }
        return PendingIntent.getActivity(context, requestCode(reminderId, slot), intent, PENDING_FLAGS)
    }

    /** Distinct per reminder and button, so one reminder's intents never replace another's. */
    private fun requestCode(reminderId: Int, slot: Int) = reminderId * SLOT_COUNT + slot

    companion object {
        /** Extra on the launcher intent naming the route `App()` should navigate to. */
        const val EXTRA_OPEN_ROUTE = "cut.the.crap.reminder.OPEN_ROUTE"

        private const val TAG = "AndroidReminderNotifier"
        private const val CHANNEL_ID = "campaign_reminders"
        private const val NOTIFICATION_TAG = "action_reminder"
        private const val PENDING_FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        private const val SLOT_BODY = 0
        private const val SLOT_X = 1
        private const val SLOT_FACEBOOK = 2
        private const val SLOT_COUNT = 3
    }
}
