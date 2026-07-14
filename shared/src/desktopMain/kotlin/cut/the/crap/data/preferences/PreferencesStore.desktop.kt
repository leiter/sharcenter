package cut.the.crap.data.preferences

import okio.Path
import okio.Path.Companion.toPath

/**
 * Per-OS application data directory — the desktop equivalent of Android's `filesDir`.
 *
 * macOS and Windows both have a conventional location; Linux follows the XDG spec, falling back to
 * `~/.local/share` when `XDG_DATA_HOME` is unset.
 */
actual fun preferencesPath(name: String): Path =
    "${appDataDirectory()}/datastore/$name.preferences_pb".toPath()

private const val APP_DIR = "ShareCare"

private fun appDataDirectory(): String {
    val home = System.getProperty("user.home")
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("mac") -> "$home/Library/Application Support/$APP_DIR"
        os.contains("win") ->
            (System.getenv("APPDATA") ?: "$home/AppData/Roaming") + "/$APP_DIR"
        else -> (System.getenv("XDG_DATA_HOME") ?: "$home/.local/share") + "/$APP_DIR"
    }
}
