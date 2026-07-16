package cut.the.crap.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import cut.the.crap.tools.defaultIoDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okio.Path

/**
 * Where a named preferences file lives on this platform.
 *
 * This is the *only* thing that actually differs per platform. The Android artifact's
 * `preferencesDataStore(name = …)` delegate is Context-bound and therefore Android-only, but the
 * multiplatform core exposes the same Preferences API and simply asks the caller for a path.
 *
 * Android reproduces the delegate's own layout — `filesDir/datastore/<name>.preferences_pb` — so an
 * existing store is picked up rather than quietly replaced by an empty one.
 */
expect fun preferencesPath(name: String): Path

/**
 * Creates a [DataStore] of [Preferences] for [name], with no reference to Android.
 *
 * The keys (`stringPreferencesKey` and friends) already come from `datastore-preferences-core`,
 * which is multiplatform — so the repositories on top of this need no change beyond swapping
 * `java.io.IOException` for `okio.IOException`.
 *
 * **Must be a singleton per file.** DataStore throws if two live instances share one path, so this
 * belongs behind a Koin `single` (which is how the three stores are already wired). [scope] owns
 * the store's lifetime and exists mainly so tests can close one store before opening another over
 * the same file.
 */
fun createPreferencesStore(
    name: String,
    path: Path = preferencesPath(name),
    scope: CoroutineScope = CoroutineScope(defaultIoDispatcher + SupervisorJob()),
): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(scope = scope, produceFile = { path })
