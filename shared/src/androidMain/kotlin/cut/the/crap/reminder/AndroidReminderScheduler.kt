package cut.the.crap.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import cut.the.crap.platform.ReminderScheduler
import java.util.concurrent.TimeUnit

/**
 * Runs [ReminderWorker] as unique periodic work. WorkManager persists the schedule itself, across
 * reboots and app updates, so this only needs calling when reminders change (see
 * [ReminderScheduleSync]).
 *
 * `KEEP` rather than `UPDATE`: re-enqueueing on every app start must not reset the period, or a
 * user who opens the app often would keep pushing the next run out.
 */
class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {

    override val isSupported: Boolean = true

    override fun ensureScheduled() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(
            DISPATCH_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        ).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "action_reminder_dispatch"

        /**
         * Half the minimum window length the create sheet allows (60 min, spec §2.2), so every
         * window gets at least one run when the device is not dozing.
         */
        const val DISPATCH_INTERVAL_MINUTES = 30L
    }
}
