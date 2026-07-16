package cut.the.crap.platform

/**
 * Desktop cannot cleanly relaunch its own JVM process, so restart is unsupported. The database
 * restore path checks [isSupported] and instead asks the user to relaunch manually. Mirrors the
 * capability-flag shape of [DesktopLoginFlow] / [DesktopSharer].
 */
class DesktopAppRestarter : AppRestarter {
    override val isSupported: Boolean = false
    override fun restart() { /* no-op — see isSupported */ }
}
