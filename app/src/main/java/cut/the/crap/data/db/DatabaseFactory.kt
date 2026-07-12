package cut.the.crap.data.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import cut.the.crap.data.db.sql.Content_items_table
import cut.the.crap.data.db.sql.Handle_tag_table
import cut.the.crap.data.db.sql.Link_subject_cross_ref
import cut.the.crap.data.db.sql.Post_subject_cross_ref
import cut.the.crap.data.db.sql.ShareDatabase
import cut.the.crap.data.db.sql.Subjects_table
import cut.the.crap.data.db.sql.Tweets_table

/** Physical database file name — unchanged from Room, so existing installs keep their data. */
const val DATABASE_NAME = "app_database"

/** Schema version, kept in sync with the SQLDelight migrations (1..4.sqm -> version 5). */
const val CURRENT_SCHEMA_VERSION = 5

/**
 * Creates the Android SQLDelight driver.
 *
 * SQLDelight's schema version (5) matches the `user_version` Room already wrote, so a
 * database created by the Room build opens as-is; older databases are upgraded by the
 * `.sqm` migrations, which mirror the previous Room `Migration` objects.
 *
 * When this moves to `commonMain` (module-split phase) this becomes an `expect fun`,
 * with the JDBC driver on desktop and the native driver on iOS/macOS.
 */
fun createDriver(context: Context, name: String = DATABASE_NAME): SqlDriver = AndroidSqliteDriver(
    schema = ShareDatabase.Schema,
    context = context,
    name = name,
    callback = object : AndroidSqliteDriver.Callback(ShareDatabase.Schema) {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // Room enabled foreign keys by default; the subject cross-ref tables rely on
            // ON DELETE CASCADE, so keep enforcement on.
            db.execSQL("PRAGMA foreign_keys=ON;")
        }
    },
)

/** Builds the generated database, supplying the Int<->Long column adapters. */
fun createDatabase(driver: SqlDriver): ShareDatabase = ShareDatabase(
    driver = driver,
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
