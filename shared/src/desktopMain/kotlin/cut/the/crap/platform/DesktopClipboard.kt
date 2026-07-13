package cut.the.crap.platform

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

/** The system clipboard, via AWT. */
class DesktopClipboard : Clipboard {

    private val clipboard get() = Toolkit.getDefaultToolkit().systemClipboard

    override fun copy(text: String) {
        try {
            clipboard.setContents(StringSelection(text), null)
        } catch (e: Exception) {
            Log.e(TAG, "Copy failed.", e)
        }
    }

    override fun paste(): String? = try {
        clipboard.getData(DataFlavor.stringFlavor) as? String
    } catch (e: Exception) {
        // Thrown when the clipboard holds something that is not text at all.
        Log.d(TAG, "Clipboard holds no text: ${e.message}")
        null
    }

    private companion object {
        const val TAG = "DesktopClipboard"
    }
}
