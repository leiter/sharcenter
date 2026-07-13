package cut.the.crap.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.awt.FileDialog
import java.awt.Frame

/**
 * Picks files with an AWT [FileDialog], which gives the real native chooser on macOS and Windows.
 *
 * [mimeTypes] is ignored: AWT filters by filename, not MIME type, and mapping between the two
 * would be guesswork. Showing every file and validating the contents afterwards — which the
 * import and restore paths already do, because Android's Downloads provider forces it — is
 * honest and behaves the same on both platforms.
 */
@Composable
actual fun rememberFilePicker(
    mimeTypes: List<String>,
    allowMultiple: Boolean,
    onPicked: (List<PlatformUri>) -> Unit,
): FilePickerLauncher {
    // The dialog is shown later, from a click; read the latest callback rather than capturing
    // the one that happened to exist at composition time.
    val currentOnPicked by rememberUpdatedState(onPicked)

    return remember(allowMultiple) {
        FilePickerLauncher {
            val dialog = FileDialog(null as Frame?, "Choose a file", FileDialog.LOAD).apply {
                isMultipleMode = allowMultiple
                isVisible = true // blocks until the user chooses or cancels
            }
            // `files` is empty when the user cancelled.
            currentOnPicked(dialog.files.map { PlatformUri(it.absolutePath) })
        }
    }
}
