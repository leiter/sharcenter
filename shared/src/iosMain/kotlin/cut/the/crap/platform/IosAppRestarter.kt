package cut.the.crap.platform

/**
 * iOS applications cannot relaunch their own process, so restart is unsupported (as on desktop).
 * The database-restore path checks [isSupported] and instead asks the user to reopen the app.
 */
class IosAppRestarter : AppRestarter {
    override val isSupported: Boolean = false
    override fun restart() { /* no-op — see isSupported */ }
}
