package cut.the.crap.data.domain

import cut.the.crap.data.db.ActionReminderDB
import cut.the.crap.data.db.ActionReminderDao
import cut.the.crap.tools.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

interface ActionReminderRepository {
    /** All reminders, oldest first. */
    fun observeAll(): Flow<List<ActionReminder>>

    suspend fun getEnabled(): List<ActionReminder>

    suspend fun getById(id: Int): ActionReminder?

    /** Stores [reminder] as a new row and returns its id. */
    suspend fun insert(reminder: ActionReminder): Int

    suspend fun update(reminder: ActionReminder)

    suspend fun delete(id: Int)

    suspend fun setEnabled(id: Int, enabled: Boolean)

    /** Bookkeeping after a notification was shown: rotation, fire time, and — for `Once` — disabling. */
    suspend fun recordFired(id: Int, nextPostIndex: Int, firedAt: Long, enabled: Boolean)
}

class ActionReminderRepositoryImpl(
    private val dao: ActionReminderDao,
) : ActionReminderRepository {

    override fun observeAll(): Flow<List<ActionReminder>> =
        dao.getAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getEnabled(): List<ActionReminder> =
        dao.getEnabled().map { it.toDomain() }

    override suspend fun getById(id: Int): ActionReminder? =
        dao.getById(id)?.toDomain()

    override suspend fun insert(reminder: ActionReminder): Int =
        dao.insert(reminder.toDbItem()).toInt()

    override suspend fun update(reminder: ActionReminder) =
        dao.update(reminder.copy(modifiedAt = currentTimeMillis()).toDbItem())

    override suspend fun delete(id: Int) = dao.delete(id)

    override suspend fun setEnabled(id: Int, enabled: Boolean) =
        dao.setEnabled(id, enabled, currentTimeMillis())

    override suspend fun recordFired(id: Int, nextPostIndex: Int, firedAt: Long, enabled: Boolean) =
        dao.recordFired(id, nextPostIndex, firedAt, enabled)
}

// --- Row <-> domain mapping ---

private const val SCHEDULE_RECURRING = "RECURRING"
private const val SCHEDULE_ONCE = "ONCE"
private const val MINUTES_PER_DAY = 24 * 60

private val reminderJson = Json { ignoreUnknownKeys = true }
private val postsSerializer = ListSerializer(ReminderPost.serializer())

/**
 * Corrupt columns degrade instead of throwing — this runs inside the list screen's flow and the
 * background worker, where one bad row must not take down every other reminder. Unreadable posts
 * load as an empty list (the dispatcher then skips the reminder), and a `ONCE` row with an
 * unreadable date loads as a recurring reminder with no days, which never fires.
 */
internal fun ActionReminderDB.toDomain(): ActionReminder {
    val window = TimeWindow(localTimeOfMinute(windowStartMinute), localTimeOfMinute(windowEndMinute))
    val date = onceDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val schedule = if (scheduleType == SCHEDULE_ONCE && date != null) {
        ReminderSchedule.Once(date, window)
    } else {
        ReminderSchedule.Recurring(daysFromMask(daysOfWeek), window)
    }
    return ActionReminder(
        id = id,
        campaignId = campaignId,
        countryCode = countryCode,
        language = language,
        postToX = postToX,
        postToFacebook = postToFacebook,
        schedule = schedule,
        posts = runCatching { reminderJson.decodeFromString(postsSerializer, postsJson) }
            .getOrDefault(emptyList()),
        campaignVersion = campaignVersion,
        nextPostIndex = nextPostIndex,
        enabled = enabled,
        lastFiredAt = lastFiredAt,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
    )
}

internal fun ActionReminder.toDbItem(): ActionReminderDB = ActionReminderDB(
    id = id,
    campaignId = campaignId,
    countryCode = countryCode,
    language = language,
    postToX = postToX,
    postToFacebook = postToFacebook,
    scheduleType = when (schedule) {
        is ReminderSchedule.Recurring -> SCHEDULE_RECURRING
        is ReminderSchedule.Once -> SCHEDULE_ONCE
    },
    daysOfWeek = (schedule as? ReminderSchedule.Recurring)?.days?.let(::maskFromDays) ?: 0,
    onceDate = (schedule as? ReminderSchedule.Once)?.date?.toString(),
    windowStartMinute = schedule.window.start.minuteOfDay(),
    windowEndMinute = schedule.window.end.minuteOfDay(),
    postsJson = reminderJson.encodeToString(postsSerializer, posts),
    campaignVersion = campaignVersion,
    nextPostIndex = nextPostIndex,
    enabled = enabled,
    lastFiredAt = lastFiredAt,
    createdAt = createdAt,
    modifiedAt = modifiedAt,
)

internal fun maskFromDays(days: Set<DayOfWeek>): Int =
    days.fold(0) { mask, day -> mask or (1 shl (day.isoDayNumber - 1)) }

internal fun daysFromMask(mask: Int): Set<DayOfWeek> =
    DayOfWeek.entries.filter { mask and (1 shl (it.isoDayNumber - 1)) != 0 }.toSet()

private fun localTimeOfMinute(minute: Int): LocalTime {
    val clamped = minute.coerceIn(0, MINUTES_PER_DAY - 1)
    return LocalTime(clamped / 60, clamped % 60)
}

private fun LocalTime.minuteOfDay(): Int = hour * 60 + minute
