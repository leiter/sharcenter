package cut.the.crap.platform

import androidx.compose.runtime.Composable

/** Opens the platform's file chooser. Returned by [rememberFilePicker]. */
fun interface FilePickerLauncher {
    fun launch()
}

/**
 * Lets the user choose files, and hands back opaque [PlatformUri] handles that [FileAccess] can
 * read.
 *
 * This is the one seam that has to be a **composable** rather than an injected interface: on
 * Android, picking a file means registering an activity-result contract, and registration has to
 * happen during composition (`rememberLauncherForActivityResult`), before anything is launched.
 * An injected object could not do that. So the seam takes the shape of the thing it is hiding.
 *
 * [onPicked] receives an empty list when the user cancels. Single-file callers take
 * `firstOrNull()`.
 *
 * @param mimeTypes MIME types to offer, e.g. `["text/plain"]`. The wildcard glob accepts anything,
 *   which is necessary in practice: Android's Downloads provider hands out `.csv` and `.db` files
 *   as `application/octet-stream`, so filtering by their real type would hide them.
 */
@Composable
expect fun rememberFilePicker(
    mimeTypes: List<String>,
    allowMultiple: Boolean = false,
    onPicked: (List<PlatformUri>) -> Unit,
): FilePickerLauncher
