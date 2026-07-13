package cut.the.crap.platform

import android.util.Log as AndroidLog

/** Delegates straight to Logcat, so logging behaves exactly as it did before the seam. */
actual object Log {
    actual fun v(tag: String, message: String, throwable: Throwable?) {
        if (throwable == null) AndroidLog.v(tag, message) else AndroidLog.v(tag, message, throwable)
    }

    actual fun d(tag: String, message: String, throwable: Throwable?) {
        if (throwable == null) AndroidLog.d(tag, message) else AndroidLog.d(tag, message, throwable)
    }

    actual fun i(tag: String, message: String, throwable: Throwable?) {
        if (throwable == null) AndroidLog.i(tag, message) else AndroidLog.i(tag, message, throwable)
    }

    actual fun w(tag: String, message: String, throwable: Throwable?) {
        if (throwable == null) AndroidLog.w(tag, message) else AndroidLog.w(tag, message, throwable)
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable == null) AndroidLog.e(tag, message) else AndroidLog.e(tag, message, throwable)
    }
}
