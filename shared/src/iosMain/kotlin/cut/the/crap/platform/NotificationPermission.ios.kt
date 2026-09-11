package cut.the.crap.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Action reminders are not implemented on iOS yet (spec §5.3), so there is nothing to grant.
 * The real version would call `UNUserNotificationCenter.requestAuthorization`.
 */
@Composable
actual fun rememberNotificationPermissionRequester(
    onResult: (granted: Boolean) -> Unit,
): NotificationPermissionRequester {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember { NotificationPermissionRequester { currentOnResult(false) } }
}
