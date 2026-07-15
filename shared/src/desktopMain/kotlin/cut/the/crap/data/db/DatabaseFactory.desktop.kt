package cut.the.crap.data.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import cut.the.crap.data.db.sql.ShareDatabase
import java.io.File

/**
 * Desktop (JVM) SQLDelight driver — the counterpart to the Android [createDriver], using the JDBC
 * SQLite driver the migration tests already run against.
 *
 * Unlike `AndroidSqliteDriver` (whose callback creates/migrates the schema), `JdbcSqliteDriver`
 * does neither, so we drive it here off `PRAGMA user_version`: a fresh file (version 0) gets
 * `Schema.create`; an older one is migrated up. Foreign keys are enabled for the `ON DELETE
 * CASCADE` the subject cross-ref tables rely on — matching the Android callback.
 */
fun createDriver(databaseFile: File): SqlDriver {
    databaseFile.parentFile?.mkdirs()
    val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
    driver.execute(null, "PRAGMA foreign_keys=ON", 0)

    val current = driver.executeQuery(
        identifier = null,
        sql = "PRAGMA user_version",
        mapper = { cursor -> cursor.next(); QueryResult.Value(cursor.getLong(0)!!) },
        parameters = 0,
    ).value
    val target = ShareDatabase.Schema.version

    when {
        current == 0L -> {
            ShareDatabase.Schema.create(driver).value
            driver.execute(null, "PRAGMA user_version = $target", 0)
        }
        current < target -> {
            ShareDatabase.Schema.migrate(driver, current, target).value
            driver.execute(null, "PRAGMA user_version = $target", 0)
        }
    }
    return driver
}
