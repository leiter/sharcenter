package cut.the.crap.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import cut.the.crap.data.db.sql.ShareDatabase

/**
 * iOS SQLDelight driver (SQLite via sqliter). Foreign keys are enabled to match Android/desktop —
 * the subject cross-ref tables rely on ON DELETE CASCADE. `createDatabase(driver)` (commonMain)
 * supplies the column adapters, shared with every target.
 */
fun createDriver(name: String = DATABASE_NAME): SqlDriver = NativeSqliteDriver(
    schema = ShareDatabase.Schema,
    name = name,
    onConfiguration = { config ->
        config.copy(
            extendedConfig = config.extendedConfig.copy(foreignKeyConstraints = true),
        )
    },
)
