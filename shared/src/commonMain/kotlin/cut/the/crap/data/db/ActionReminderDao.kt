package cut.the.crap.data.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import cut.the.crap.data.db.sql.ActionReminderQueries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * A row of `action_reminders_table`, column for column so the generated `SELECT *` mapper can
 * take the constructor directly. The domain shape (typed schedule, decoded posts) is
 * [cut.the.crap.data.domain.ActionReminder].
 */
data class ActionReminderDB(
    val id: Int,
    val campaignId: String,
    val countryCode: String,
    val language: String,
    val postToX: Boolean,
    val postToFacebook: Boolean,
    val scheduleType: String,
    val daysOfWeek: Int,
    val onceDate: String?,
    val windowStartMinute: Int,
    val windowEndMinute: Int,
    val postsJson: String,
    val campaignVersion: Int,
    val nextPostIndex: Int,
    val enabled: Boolean,
    val lastFiredAt: Long?,
    val createdAt: Long,
    val modifiedAt: Long,
)

interface ActionReminderDao {
    /** Inserts a new row — [ActionReminderDB.id] is ignored — and returns the id SQLite assigned. */
    suspend fun insert(reminder: ActionReminderDB): Long

    suspend fun update(reminder: ActionReminderDB)

    suspend fun delete(reminderId: Int)

    suspend fun getById(reminderId: Int): ActionReminderDB?

    /** All reminders, oldest first — the order the user created them in. */
    fun getAll(): Flow<List<ActionReminderDB>>

    suspend fun getEnabled(): List<ActionReminderDB>

    suspend fun setEnabled(reminderId: Int, enabled: Boolean, modifiedAt: Long)

    suspend fun recordFired(reminderId: Int, nextPostIndex: Int, lastFiredAt: Long, enabled: Boolean)
}

class SqlDelightActionReminderDao(
    private val queries: ActionReminderQueries,
    private val dispatcher: CoroutineDispatcher,
) : ActionReminderDao {

    override suspend fun insert(reminder: ActionReminderDB): Long = withContext(dispatcher) {
        with(reminder) {
            queries.transactionWithResult {
                queries.insert(
                    campaignId, countryCode, language, postToX, postToFacebook, scheduleType,
                    daysOfWeek, onceDate, windowStartMinute, windowEndMinute, postsJson,
                    campaignVersion, nextPostIndex, enabled, lastFiredAt, createdAt, modifiedAt,
                )
                queries.lastInsertRowId().executeAsOne()
            }
        }
    }

    override suspend fun update(reminder: ActionReminderDB) = withContext(dispatcher) {
        with(reminder) {
            queries.update(
                campaignId, countryCode, language, postToX, postToFacebook, scheduleType,
                daysOfWeek, onceDate, windowStartMinute, windowEndMinute, postsJson,
                campaignVersion, nextPostIndex, enabled, lastFiredAt, createdAt, modifiedAt, id,
            )
        }
    }

    override suspend fun delete(reminderId: Int) = withContext(dispatcher) {
        queries.delete(reminderId)
    }

    override suspend fun getById(reminderId: Int): ActionReminderDB? = withContext(dispatcher) {
        queries.getById(reminderId, ::ActionReminderDB).executeAsOneOrNull()
    }

    override fun getAll(): Flow<List<ActionReminderDB>> =
        queries.getAll(::ActionReminderDB).asFlow().mapToList(dispatcher)

    override suspend fun getEnabled(): List<ActionReminderDB> = withContext(dispatcher) {
        queries.getEnabled(::ActionReminderDB).executeAsList()
    }

    override suspend fun setEnabled(reminderId: Int, enabled: Boolean, modifiedAt: Long) =
        withContext(dispatcher) {
            queries.setEnabled(enabled, modifiedAt, reminderId)
        }

    override suspend fun recordFired(
        reminderId: Int,
        nextPostIndex: Int,
        lastFiredAt: Long,
        enabled: Boolean,
    ) = withContext(dispatcher) {
        queries.recordFired(nextPostIndex, lastFiredAt, enabled, reminderId)
    }
}
