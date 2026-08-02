package cut.the.crap.platform

import android.content.Context
import android.content.Intent

/**
 * Relaunches the app by starting a fresh launch intent in a new, cleared task and killing the
 * current process. Was `MainActivity.restartApp`; moved here so the app-root action handler can
 * stay in `commonMain`.
 */
class AndroidAppRestarter(private val context: Context) : AppRestarter {

    override val isSupported: Boolean = true

    override fun restart() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
