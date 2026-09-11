package cut.the.crap.data.db

import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import cut.the.crap.data.db.sql.Action_reminders_table
import cut.the.crap.data.db.sql.Content_items_table
import cut.the.crap.data.db.sql.Handle_tag_table
import cut.the.crap.data.db.sql.Link_subject_cross_ref
import cut.the.crap.data.db.sql.Post_subject_cross_ref
import cut.the.crap.data.db.sql.ShareDatabase
import cut.the.crap.data.db.sql.Subjects_table
import cut.the.crap.data.db.sql.Tweets_table

/** Physical database file name — unchanged from Room, so existing installs keep their data. */
const val DATABASE_NAME = "app_database"

/**
 * Schema version of this build, read from the generated schema rather than hardcoded — a
 * hardcoded copy was left at 5 after `5.sqm` landed, and the backup restore guard then rejected
 * every backup this build made as "from a newer app version".
 */
val CURRENT_SCHEMA_VERSION: Long get() = ShareDatabase.Schema.version

/**
 * Builds the generated database from a platform-provided [SqlDriver], supplying the
 * Int<->Long column adapters (Boolean is handled natively by SQLDelight).
 *
 * This is platform-agnostic: the driver is created per platform
 * (`AndroidSqliteDriver`, `JdbcSqliteDriver` on desktop, `NativeSqliteDriver` on Apple).
 */
fun createDatabase(driver: SqlDriver): ShareDatabase = ShareDatabase(
    driver = driver,
    action_reminders_tableAdapter = Action_reminders_table.Adapter(
        idAdapter = IntColumnAdapter,
        daysOfWeekAdapter = IntColumnAdapter,
        windowStartMinuteAdapter = IntColumnAdapter,
        windowEndMinuteAdapter = IntColumnAdapter,
        campaignVersionAdapter = IntColumnAdapter,
        nextPostIndexAdapter = IntColumnAdapter,
    ),
    content_items_tableAdapter = Content_items_table.Adapter(
        idAdapter = IntColumnAdapter,
        sortOrderAdapter = IntColumnAdapter,
    ),
    handle_tag_tableAdapter = Handle_tag_table.Adapter(
        idAdapter = IntColumnAdapter,
        typeAdapter = IntColumnAdapter,
        usageCountAdapter = IntColumnAdapter,
        sortOrderAdapter = IntColumnAdapter,
    ),
    link_subject_cross_refAdapter = Link_subject_cross_ref.Adapter(
        linkIdAdapter = IntColumnAdapter,
        subjectIdAdapter = IntColumnAdapter,
    ),
    post_subject_cross_refAdapter = Post_subject_cross_ref.Adapter(
        postIdAdapter = IntColumnAdapter,
        subjectIdAdapter = IntColumnAdapter,
    ),
    subjects_tableAdapter = Subjects_table.Adapter(
        idAdapter = IntColumnAdapter,
    ),
    tweets_tableAdapter = Tweets_table.Adapter(
        idAdapter = IntColumnAdapter,
        positionAdapter = IntColumnAdapter,
    ),
)
