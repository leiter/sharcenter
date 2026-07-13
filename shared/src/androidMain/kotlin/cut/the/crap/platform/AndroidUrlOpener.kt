package cut.the.crap.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Opens URLs with `ACTION_VIEW`.
 *
 * [preferApp] is honoured by pinning the intent to that app's package when it is installed. The
 * install check lives here rather than behind its own seam because "is X installed" is not a
 * question shared code ever needs to ask — it only ever wanted the URL opened, preferably in the
 * native client. If the app is absent we drop the package pin and the browser takes it.
 */
class AndroidUrlOpener(private val context: Context) : UrlOpener {

    override fun open(url: String, preferApp: ExternalApp?) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            // The injected Context is the Application, which has no task to launch into.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            preferApp?.packageName()?.let { pkg ->
                if (isInstalled(pkg)) setPackage(pkg)
            }
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Nothing could open: $url", e)
        }
    }

    private fun isInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private fun ExternalApp.packageName(): String = when (this) {
        ExternalApp.X -> "com.twitter.android"
        ExternalApp.Facebook -> "com.facebook.katana"
        ExternalApp.Instagram -> "com.instagram.android"
    }

    private companion object {
        const val TAG = "AndroidUrlOpener"
    }
}
