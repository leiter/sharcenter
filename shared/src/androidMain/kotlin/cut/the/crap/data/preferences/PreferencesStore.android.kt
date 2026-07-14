package cut.the.crap.data.preferences

import android.content.Context
import okio.Path
import okio.Path.Companion.toPath

/**
 * Set once at startup, before any store is created. A plain holder rather than DI, because the
 * path is needed by a top-level `expect fun` that cannot take a Koin scope.
 */
private var appContext: Context? = null

fun initPreferencesPath(context: Context) {
    appContext = context.applicationContext
}

/**
 * Exactly the layout `preferencesDataStore(name = …)` uses: `filesDir/datastore/<name>
 * .preferences_pb`. Matching it means the existing store is opened, not orphaned.
 */
actual fun preferencesPath(name: String): Path {
    val context = requireNotNull(appContext) {
        "initPreferencesPath() must be called before any preferences store is created."
    }
    return "${context.filesDir.path}/datastore/$name.preferences_pb".toPath()
}
