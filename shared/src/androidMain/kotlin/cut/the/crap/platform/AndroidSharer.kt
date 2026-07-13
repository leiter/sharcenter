package cut.the.crap.platform

import android.content.Context
import android.content.Intent

/** The Android share sheet (`ACTION_SEND` wrapped in a chooser). */
class AndroidSharer(private val context: Context) : Sharer {

    override val isSupported: Boolean = true

    override fun shareText(text: String, chooserTitle: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        // The injected Context is the Application, which has no task to launch into.
        val chooser = Intent.createChooser(send, chooserTitle)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Share sheet could not be shown.", e)
        }
    }

    private companion object {
        const val TAG = "AndroidSharer"
    }
}
