package cut.the.crap.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Routes the platform asks the UI to open from outside the composition — today, a reminder
 * notification's body tap.
 *
 * A [StateFlow] rather than an event stream: on a cold start the request arrives in
 * `Activity.onCreate`, before `App()` has composed its `NavHost`, and it must still be there when
 * it has. `App()` navigates and then [consume]s it.
 */
class NavigationRequests {

    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun request(route: String) {
        _pending.value = route
    }

    /** Clears [route] once handled — unless a newer request replaced it in the meantime. */
    fun consume(route: String) {
        _pending.compareAndSet(route, null)
    }
}
