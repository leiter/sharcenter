package cut.the.crap.platform

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Toast-backed [Notifier].
 *
 * Toast must be raised on the main looper. Callers today are all on the main dispatcher, but
 * that is an easy invariant to break from a background coroutine — and the failure is a crash,
 * not a dropped message — so the hop is made explicit here rather than assumed.
 */
class AndroidNotifier(private val context: Context) : Notifier {

    private val main = Handler(Looper.getMainLooper())

    override fun show(message: String, duration: NotificationDuration) {
        val length = when (duration) {
            NotificationDuration.Short -> Toast.LENGTH_SHORT
            NotificationDuration.Long -> Toast.LENGTH_LONG
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(context, message, length).show()
        } else {
            main.post { Toast.makeText(context, message, length).show() }
        }
    }
}
