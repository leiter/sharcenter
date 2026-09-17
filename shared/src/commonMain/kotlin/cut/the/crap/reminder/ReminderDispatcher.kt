@file:OptIn(kotlin.time.ExperimentalTime::class)

package cut.the.crap.reminder

import cut.the.crap.data.domain.ActionReminder
import cut.the.crap.data.domain.ActionReminderRepository
import cut.the.crap.data.domain.ReminderSchedule
import cut.the.crap.intent.FacebookIntent
import cut.the.crap.intent.TwitterIntent
import cut.the.crap.platform.ReminderNotification
import cut.the.crap.platform.ReminderNotifier
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

/**
 * The user's hidden campaign posts (`campaignPostHideKey` format).
 *
 * A `fun interface` rather than a bare `suspend () -> Set<String>` so tests need no DataStore
 * (any lambda still SAM-converts at the call site) — and, unlike a raw suspend function type, its
 * classifier reflects cleanly: Koin's static `verify()` reads every bound class's constructor via
 * `kotlin-reflect`, and a suspend function type's classifier comes back null there, which crashes
 * with a `ClassCastException` rather than a normal "missing definition" report.
 */
fun interface HiddenPostKeys {
    suspend fun get(): Set<String>
}

/**
 * Decides which reminders fire and shows them — spec §4.3.
 *
 * All the timing and rotation logic lives here in common code so it is testable on the desktop
 * target; the Android worker is a thin shell that calls [dispatch].
 */
class ReminderDispatcher(
    private val repository: ActionReminderRepository,
    private val notifier: ReminderNotifier,
    private val hiddenPostKeys: HiddenPostKeys,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {

    /**
     * Shows every reminder due at [now] and returns how many notifications went out.
     *
     * Nothing fires while notifications are blocked: showing would be a no-op, and recording it
     * would silently advance rotations and use up one-off reminders the user never saw.
     */
    suspend fun dispatch(now: Instant): Int {
        if (!notifier.canNotify) return 0
        val tz = timeZone()
        val due = repository.getEnabled().filter { it.isDue(now, tz) }
        if (due.isEmpty()) return 0
        val hidden = hiddenPostKeys.get()
        return due.count { fire(it, now, hidden, spendOnce = true) }
    }

    /**
     * "Send now" from the reminder list: fires [reminderId] regardless of its schedule. A one-off
     * reminder stays enabled — this is a preview, not its occurrence.
     */
    suspend fun sendNow(reminderId: Int, now: Instant): Boolean {
        if (!notifier.canNotify) return false
        val reminder = repository.getById(reminderId) ?: return false
        return fire(reminder, now, hiddenPostKeys.get(), spendOnce = false)
    }

    private suspend fun fire(
        reminder: ActionReminder,
        now: Instant,
        hidden: Set<String>,
        spendOnce: Boolean,
    ): Boolean {
        val visible = reminder.visiblePosts(hidden)
        // Every post hidden: nothing worth showing. Rotation is left alone so un-hiding resumes it.
        if (visible.isEmpty()) return false

        val position = reminder.nextPostIndex.mod(visible.size)
        val post = visible[position]
        notifier.show(
            ReminderNotification(
                reminderId = reminder.id,
                countryCode = reminder.countryCode,
                postLabel = post.id,
                text = post.text,
                xUrl = if (reminder.postToX) TwitterIntent.PostTweet(text = post.text).url else null,
                facebookUrl = if (reminder.postToFacebook) {
                    FacebookIntent.SharePost(text = post.text).url
                } else {
                    null
                },
            ),
        )

        val spent = spendOnce && reminder.schedule is ReminderSchedule.Once
        repository.recordFired(
            id = reminder.id,
            nextPostIndex = (position + 1) % visible.size,
            firedAt = now.toEpochMilliseconds(),
            enabled = reminder.enabled && !spent,
        )
        return true
    }
}
