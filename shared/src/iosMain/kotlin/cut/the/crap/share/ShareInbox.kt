@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package cut.the.crap.share

import cut.the.crap.platform.Notifier
import cut.the.crap.platform.Log
import cut.the.crap.shared.resources.Res
import cut.the.crap.shared.resources.share_toast_handle_exists
import cut.the.crap.shared.resources.share_toast_handle_saved
import cut.the.crap.shared.resources.share_toast_link_resolved_saved
import cut.the.crap.shared.resources.share_toast_link_saved
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.FileSystem
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.getString
import org.koin.mp.KoinPlatform
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL

/**
 * The **App Group** shared identifier — must match the `com.apple.security.application-groups`
 * entitlement on BOTH the app and the Share Extension, and the `appGroupId` in the extension's
 * Swift. Change all four together or the extension's writes land somewhere the app can't read.
 */
const val APP_GROUP_ID = "group.cut.the.crap"

/** One captured share awaiting processing: [id] is the inbox file name, [url] its contents. */
data class PendingShare(val id: String, val url: String)

/**
 * The app-side of the iOS Share Extension hand-off. The extension (a separate process) writes each
 * shared URL as a file under `<AppGroup>/inbox/`; this reads and clears that inbox. One file per
 * share (rather than a shared list) sidesteps cross-process append races and is crash-safe.
 *
 * The actual save/resolve/enrich is not done here — see [drainShareInbox], which feeds each URL
 * through the same [SharedUrlProcessor] the in-app share path uses.
 */
object ShareInbox {

    private val fs: FileSystem get() = FileSystem.SYSTEM

    /**
     * `<AppGroup container>/inbox`, created if missing. Null when the App Group container is
     * unavailable (entitlement not provisioned) — the drain then simply no-ops.
     */
    private fun inboxDirectory(): String? {
        val container: NSURL = NSFileManager.defaultManager
            .containerURLForSecurityApplicationGroupIdentifier(APP_GROUP_ID) ?: return null
        val inbox = container.URLByAppendingPathComponent("inbox", isDirectory = true) ?: return null
        NSFileManager.defaultManager.createDirectoryAtURL(inbox, true, null, null)
        return inbox.path
    }

    /** Every captured share currently in the inbox. Empty (never throws) if the inbox is absent. */
    fun pending(): List<PendingShare> {
        val dirPath = inboxDirectory() ?: return emptyList()
        val dir = dirPath.toPath()
        if (!fs.exists(dir)) return emptyList()
        return try {
            fs.list(dir)
                .filter { it.name.endsWith(".txt") }
                .mapNotNull { path ->
                    val url = fs.read(path) { readUtf8() }.trim()
                    if (url.isEmpty()) null else PendingShare(id = path.name, url = url)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading share inbox", e)
            emptyList()
        }
    }

    /** Removes a processed entry by its [PendingShare.id]. */
    fun remove(id: String) {
        val dirPath = inboxDirectory() ?: return
        try {
            val file = "$dirPath/$id".toPath()
            if (fs.exists(file)) fs.delete(file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove inbox entry: $id", e)
        }
    }

    private const val TAG = "ShareInbox"
}

private val drainScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
private val drainMutex = Mutex()

/**
 * Drains the Share-Extension inbox: for each captured URL, run the same non-interactive save path
 * the in-app share flow uses ([SharedUrlProcessor.resolve] → [SharedUrlProcessor.saveHandle] or
 * [SharedUrlProcessor.saveLink]) and surface the result via [Notifier] (the snackbar host in the
 * shared `App()`). Exported to Swift — call it from `iOSApp` when the scene becomes active.
 *
 * Fire-and-forget and safe to call repeatedly: a [Mutex] serialises overlapping calls (startup vs
 * foreground), and an empty inbox is a no-op. Resolves [SharedUrlProcessor]/[Notifier] from the
 * already-started app Koin — no new modules. Interactive steps the extension can't do are skipped:
 * an `AuthRequired` URL (iOS has no X sign-in) is saved unresolved, and there is no edit dialog.
 * Each entry is removed after its attempt (at-most-once; a transient failure is not retried).
 */
fun drainShareInbox() {
    drainScope.launch {
        drainMutex.withLock {
            val koin = KoinPlatform.getKoinOrNull() ?: return@withLock
            val processor = koin.get<SharedUrlProcessor>()
            val notifier = koin.get<Notifier>()

            for (item in ShareInbox.pending()) {
                try {
                    processSharedUrl(processor, notifier, item.url)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("ShareInbox", "Failed to process shared url", e)
                }
                ShareInbox.remove(item.id)
            }
        }
    }
}

private suspend fun processSharedUrl(processor: SharedUrlProcessor, notifier: Notifier, rawUrl: String) {
    val url: String
    val wasResolved: Boolean
    when (val resolution = processor.resolve(rawUrl)) {
        is ResolveResult.Ready -> {
            url = resolution.url
            wasResolved = resolution.wasResolved
        }
        // iOS has no interactive X sign-in, so an auth-gated URL is saved unresolved.
        is ResolveResult.AuthRequired -> {
            url = resolution.url
            wasResolved = false
        }
    }

    val handle = processor.handleToSaveInstead(url)
    if (handle != null) {
        val result = processor.saveHandle(handle)
        val message =
            if (result.alreadyExisted) getString(Res.string.share_toast_handle_exists, result.handle)
            else getString(Res.string.share_toast_handle_saved, result.handle)
        notifier.show(message)
    } else {
        val result = processor.saveLink(url, wasResolved = wasResolved)
        val message =
            if (result.wasResolved) getString(Res.string.share_toast_link_resolved_saved)
            else getString(Res.string.share_toast_link_saved)
        notifier.show(message)
    }
}
