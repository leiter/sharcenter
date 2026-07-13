package cut.the.crap.platform

/**
 * Logging seam.
 *
 * Deliberately mirrors `android.util.Log`'s shape (`Log.d(TAG, msg)`), so moving a file onto it
 * is a one-line import swap rather than a rewrite of every call site.
 *
 * Android delegates to `android.util.Log`; desktop writes to stdout/stderr. iOS would only need
 * a third `actual` — no call site changes.
 */
expect object Log {
    fun v(tag: String, message: String, throwable: Throwable? = null)
    fun d(tag: String, message: String, throwable: Throwable? = null)
    fun i(tag: String, message: String, throwable: Throwable? = null)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
