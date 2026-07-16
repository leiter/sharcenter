package cut.the.crap.data.preferences

import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * DataStore file under the app's Documents directory — the iOS equivalent of Android's `filesDir`.
 * The `datastore/` subdirectory and `.preferences_pb` file are created by DataStore on first write.
 */
actual fun preferencesPath(name: String): Path {
    val documents = NSSearchPathForDirectoriesInDomains(
        directory = NSDocumentDirectory,
        domainMask = NSUserDomainMask,
        expandTilde = true,
    ).first() as String
    return "$documents/datastore/$name.preferences_pb".toPath()
}
