package cut.the.crap.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/** The system clipboard, via `ClipboardManager`. */
class AndroidClipboard(private val context: Context) : Clipboard {

    private val manager: ClipboardManager?
        get() = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    override fun copy(text: String) {
        manager?.setPrimaryClip(ClipData.newPlainText(LABEL, text))
            ?: Log.w(TAG, "No ClipboardManager; copy dropped.")
    }

    override fun paste(): String? {
        val clip = manager?.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).text?.toString()?.takeIf { it.isNotEmpty() }
    }

    private companion object {
        const val TAG = "AndroidClipboard"
        const val LABEL = "Copied Text"
    }
}
