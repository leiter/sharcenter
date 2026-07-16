package cut.the.crap.tools

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The dispatcher for blocking IO (database, file, DataStore, network) work.
 *
 * `Dispatchers.IO` is JVM-only — on Kotlin/Native it is `internal` — so common code cannot name it
 * directly. This seam resolves to `Dispatchers.IO` on Android/desktop and `Dispatchers.Default` on
 * Apple targets (Kotlin/Native has no dedicated IO pool; `Default` is the documented substitute).
 * It is only a *default*: DI still injects a dispatcher where one is wanted for tests.
 */
expect val defaultIoDispatcher: CoroutineDispatcher
