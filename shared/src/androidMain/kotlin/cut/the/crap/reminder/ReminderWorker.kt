@file:OptIn(kotlin.time.ExperimentalTime::class)

package cut.the.crap.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cut.the.crap.platform.Log
import kotlinx.coroutines.CancellationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock

/**
 * The periodic job: a thin shell around [ReminderDispatcher], where all the timing and rotation
 * logic lives (and is tested). Koin is already started — WorkManager runs workers in the app
 * process, after `Application.onCreate`.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val dispatcher: ReminderDispatcher by inject()
    private val scheduleSync: ReminderScheduleSync by inject()

    override suspend fun doWork(): Result {
        try {
            val shown = dispatcher.dispatch(Clock.System.now())
            Log.d(TAG, "Showed $shown reminder(s).")
            scheduleSync.sync()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Success, not retry: a retry's backoff would compete with the next periodic run,
            // which comes within 30 minutes anyway.
            Log.e(TAG, "Reminder dispatch failed.", e)
        }
        return Result.success()
    }

    private companion object {
        const val TAG = "ReminderWorker"
    }
}
