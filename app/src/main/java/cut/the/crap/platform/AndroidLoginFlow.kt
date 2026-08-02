package cut.the.crap.platform

import android.content.Context
import android.content.Intent
import cut.the.crap.ui.XLoginActivity

/**
 * Runs the X sign-in flow by launching [XLoginActivity], which drives the WebView and persists the
 * cookies it recovers.
 *
 * This actual lives in `:app` rather than `:shared/androidMain` only because [XLoginActivity]
 * needs `SettingsRepository`, which is still in `:app` until WP5. The seam itself is in
 * `commonMain`, so nothing else is holding it here.
 */
class AndroidLoginFlow(private val context: Context) : LoginFlow {

    override val isSupported: Boolean = true

    override fun launch(reason: LoginReason) {
        val intent = XLoginActivity.createIntent(context, reason.toExtra()).apply {
            // The injected Context is the Application. XLoginActivity shares the app's task
            // affinity, so this lands it on the existing task rather than a detached one.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun LoginReason.toExtra(): String = when (this) {
        LoginReason.InitialSetup -> XLoginActivity.REASON_INITIAL_SETUP
        LoginReason.SessionExpired -> XLoginActivity.REASON_AUTH_EXPIRED
    }
}
