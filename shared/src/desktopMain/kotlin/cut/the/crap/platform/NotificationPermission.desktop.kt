package cut.the.crap.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** Desktop has no action reminder support, so there is nothing to grant. */
@Composable
actual fun rememberNotificationPermissionRequester(
    onResult: (granted: Boolean) -> Unit,
): NotificationPermissionRequester {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember { NotificationPermissionRequester { currentOnResult(false) } }
}
