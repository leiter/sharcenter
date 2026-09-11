package cut.the.crap.reminder

import cut.the.crap.data.domain.ActionReminderRepository
import cut.the.crap.platform.ReminderScheduler

/**
 * Keeps the background job's existence in step with the data: scheduled while any reminder is
 * enabled, cancelled once none is, so an app with no reminders runs no periodic work at all.
 *
 * Called on app start, after every change on the reminder screen, and by the worker itself after
 * each run — a one-off that just fired may have been the last enabled reminder.
 */
class ReminderScheduleSync(
    private val repository: ActionReminderRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend fun sync() {
        if (!scheduler.isSupported) return
        if (repository.getEnabled().isEmpty()) scheduler.cancel() else scheduler.ensureScheduled()
    }
}
