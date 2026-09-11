package cut.the.crap.platform

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * `POST_NOTIFICATIONS` on API 33+. Below that there is no runtime permission — notifications are
 * on unless the user switched them off in settings, which no dialog can change — so the request
 * reports `true` and leaves the real answer to `ReminderNotifier.canNotify`.
 *
 * Once the user has declined twice Android stops showing the dialog and the launcher answers
 * `false` immediately; the reminder screen then offers the settings page instead.
 */
@Composable
actual fun rememberNotificationPermissionRequester(
    onResult: (granted: Boolean) -> Unit,
): NotificationPermissionRequester {
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> currentOnResult(granted) }

    return remember(launcher) {
        NotificationPermissionRequester {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                currentOnResult(true)
            }
        }
    }
}
