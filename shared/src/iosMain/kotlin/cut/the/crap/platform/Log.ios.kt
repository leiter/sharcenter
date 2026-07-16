package cut.the.crap.platform

/**
 * iOS logging: writes to stdout, which Xcode's console surfaces. Dependency-free and matches the
 * desktop actual's format; a real build can swap in `os_log`/`NSLog` without touching call sites.
 */
actual object Log {
    actual fun v(tag: String, message: String, throwable: Throwable?) = out("V", tag, message, throwable)
    actual fun d(tag: String, message: String, throwable: Throwable?) = out("D", tag, message, throwable)
    actual fun i(tag: String, message: String, throwable: Throwable?) = out("I", tag, message, throwable)
    actual fun w(tag: String, message: String, throwable: Throwable?) = out("W", tag, message, throwable)
    actual fun e(tag: String, message: String, throwable: Throwable?) = out("E", tag, message, throwable)

    private fun out(level: String, tag: String, message: String, throwable: Throwable?) {
        println("$level/$tag: $message")
        throwable?.let { println(it.stackTraceToString()) }
    }
}
