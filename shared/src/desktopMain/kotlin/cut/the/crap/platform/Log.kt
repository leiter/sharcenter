package cut.the.crap.platform

/**
 * Desktop logging: stdout for v/d/i, stderr for w/e, so piping `2>` isolates problems.
 * Deliberately dependency-free — a real desktop build can swap this for SLF4J without
 * touching a single call site.
 */
actual object Log {
    actual fun v(tag: String, message: String, throwable: Throwable?) = out("V", tag, message, throwable)
    actual fun d(tag: String, message: String, throwable: Throwable?) = out("D", tag, message, throwable)
    actual fun i(tag: String, message: String, throwable: Throwable?) = out("I", tag, message, throwable)
    actual fun w(tag: String, message: String, throwable: Throwable?) = err("W", tag, message, throwable)
    actual fun e(tag: String, message: String, throwable: Throwable?) = err("E", tag, message, throwable)

    private fun out(level: String, tag: String, message: String, throwable: Throwable?) {
        println(format(level, tag, message))
        throwable?.printStackTrace(System.out)
    }

    private fun err(level: String, tag: String, message: String, throwable: Throwable?) {
        System.err.println(format(level, tag, message))
        throwable?.printStackTrace(System.err)
    }

    private fun format(level: String, tag: String, message: String) = "$level/$tag: $message"
}
