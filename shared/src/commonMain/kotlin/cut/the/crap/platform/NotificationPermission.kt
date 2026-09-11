package cut.the.crap.platform

import androidx.compose.runtime.Composable

/** Asks for permission to show notifications. Returned by [rememberNotificationPermissionRequester]. */
fun interface NotificationPermissionRequester {
    fun request()
}

/**
 * Asks the user to allow notifications — needed before action reminders can appear.
 *
 * A composable for the same reason as [rememberFilePicker]: on Android the runtime permission
 * request is an activity-result contract, which has to be registered during composition. Platforms
 * without reminder support answer `false` straight away.
 *
 * [onResult] reports what the dialog returned. Callers should still re-check
 * [ReminderNotifier.canNotify], which also covers a notification channel switched off in settings.
 */
@Composable
expect fun rememberNotificationPermissionRequester(
    onResult: (granted: Boolean) -> Unit,
): NotificationPermissionRequester
