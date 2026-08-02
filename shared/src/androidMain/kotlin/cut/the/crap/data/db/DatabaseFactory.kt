package cut.the.crap.data.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import cut.the.crap.data.db.sql.ShareDatabase

/**
 * Android SQLDelight driver.
 *
 * SQLDelight's schema version (5) matches the `user_version` Room already wrote, so a
 * database created by the Room build opens as-is; older databases are upgraded by the
 * `.sqm` migrations, which mirror the previous Room `Migration` objects.
 *
 * The desktop/iOS/macOS counterparts are `JdbcSqliteDriver` / `NativeSqliteDriver`;
 * `createDatabase(driver)` (commonMain) is shared by all of them.
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
