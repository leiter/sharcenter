package cut.the.crap.platform

/**
 * Cold-restarts the app process.
 *
 * Used after a database *restore*: the live [cut.the.crap.data.backup.BackupManager] replaces the
 * database file underneath an already-open driver, so the process must relaunch for the driver to
 * reopen the new file from scratch.
 *
 * An interface rather than `expect`/`actual` because the Android implementation needs a `Context`
 * (which an `expect` object cannot carry) — the same reason [Notifier] and friends are interfaces.
 * Platforms that cannot relaunch themselves (desktop, iOS) report [isSupported] `= false`; callers
 * check it and fall back to telling the user to relaunch.
 */
interface AppRestarter {
    /** Whether this platform can relaunch its own process. */
    val isSupported: Boolean

    /** Relaunches the app from a cold start. No-op where [isSupported] is `false`. */
    fun restart()
}
