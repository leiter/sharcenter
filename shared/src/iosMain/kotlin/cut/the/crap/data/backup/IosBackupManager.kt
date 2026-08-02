package cut.the.crap.data.backup

import app.cash.sqldelight.db.SqlDriver
import cut.the.crap.data.db.DATABASE_NAME
import cut.the.crap.data.preferences.SettingsRepository
import cut.the.crap.platform.Log
import cut.the.crap.platform.PlatformUri
import cut.the.crap.tools.currentTimeMillis
import cut.the.crap.tools.formatTimestampForFileName
import cut.the.crap.tools.toStartOfDay
import cut.the.crap.ui.content.settings.BackupFrequency
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * iOS [BackupManager]: real, local, file-based **backup** — the counterpart to Android's
 * MediaStore-backed `DatabaseBackupManager`. Backups are timestamped copies of the SQLite database
 * written into the app's Documents directory, which `UIFileSharingEnabled` /
 * `LSSupportsOpeningDocumentsInPlace` (Info.plist) make browsable in the Files app.
 *
 * **Restore is deliberately still unsupported** (see [restoreFromBackup]): replacing the live
 * database requires closing every open connection and relaunching, and `IosAppRestarter.isSupported`
 * is false — iOS apps cannot relaunch their own process. Backup alone already closes the real gap
 * (iOS previously had *no* data safety at all).
 *
 * WAL correctness: SQLite runs in WAL mode, so recently-committed pages live in the `-wal` sidecar,
 * not the main file. Before copying we run `PRAGMA wal_checkpoint(TRUNCATE)`, which flushes the WAL
 * into the main database and truncates it — so the single main file we copy is a complete,
 * self-contained, consistent database. (Android copies only the main file *without* checkpointing,
 * so this is actually stricter than the Android backup.)
 */
class IosBackupManager(
    private val driver: SqlDriver,
    private val settingsRepository: SettingsRepository,
) : BackupManager {

    private val fs: FileSystem get() = FileSystem.SYSTEM

    /**
     * Where sqliter physically stores the database: `<Application Support>/databases/<name>`. This
     * mirrors `co.touchlab.sqliter.DatabaseFileContext` (the default `basePath` used by
     * `NativeSqliteDriver` in `DatabaseFactory.ios`), so backups copy the file the app actually uses.
     */
    private fun databaseFilePath(): String =
        "${appleDirectory(NSApplicationSupportDirectory)}/databases/$DATABASE_NAME"

    /** User-visible backups directory (Documents is what the Files-sharing plist keys expose). */
    private fun backupsDirectory(): String = "${appleDirectory(NSDocumentDirectory)}/backups"

    override suspend fun listBackups(): List<BackupInfo> = withContext(Dispatchers.Default) {
        try {
            val dir = backupsDirectory().toPath()
            if (!fs.exists(dir)) return@withContext emptyList()
            fs.list(dir)
                .filter { it.name.startsWith(BACKUP_PREFIX) && it.name.endsWith(".db") }
                .map { path ->
                    val meta = fs.metadataOrNull(path)
                    BackupInfo(
                        uri = PlatformUri(path.toString()),
                        displayName = path.name,
                        sizeBytes = meta?.size ?: 0L,
                        lastModified = meta?.lastModifiedAtMillis ?: 0L,
                    )
                }
                .sortedByDescending { it.lastModified }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error listing backups", e)
            emptyList()
        }
    }

    override suspend fun deleteBackups(uris: List<PlatformUri>): Result<Int> =
        withContext(Dispatchers.Default) {
            var deleted = 0
            for (uri in uris) {
                try {
                    val path = uri.value.toPath()
                    if (fs.exists(path)) {
                        fs.delete(path)
                        deleted++
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete backup: ${uri.value}", e)
                }
            }
            Log.i(TAG, "Deleted $deleted of ${uris.size} backups")
            Result.success(deleted)
        }

    override suspend fun performManualBackup(): Result<String> = withContext(Dispatchers.Default) {
        try {
            val dbPath = databaseFilePath().toPath()
            if (!fs.exists(dbPath)) {
                Log.w(TAG, "Database file does not exist: $dbPath")
                return@withContext Result.failure(IllegalStateException("Database file not found"))
            }

            // Flush the WAL into the main file so the single file we copy is complete + consistent.
            // Best-effort: if the pragma fails we still copy — a slightly-stale backup beats none.
            try {
                driver.execute(null, "PRAGMA wal_checkpoint(TRUNCATE)", 0)
            } catch (e: Exception) {
                Log.w(TAG, "WAL checkpoint failed; copying without it", e)
            }

            val fileName = "${BACKUP_PREFIX}_${formatTimestampForFileName(currentTimeMillis())}.db"
            val dir = backupsDirectory().toPath()
            fs.createDirectories(dir)
            fs.copy(dbPath, dir / fileName)

            Log.i(TAG, "Database backup successful: $fileName")
            applyRetentionPolicy()
            Result.success(fileName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error performing backup", e)
            Result.failure(e)
        }
    }

    override suspend fun restoreFromBackup(uri: PlatformUri): Result<String> =
        // Restore needs to close the live DB, swap the file, and relaunch — but iOS apps can't
        // relaunch themselves (IosAppRestarter.isSupported = false), so restore stays unsupported.
        Result.failure(UnsupportedOperationException("Database restore is not available on iOS yet."))

    override suspend fun performDailyBackupIfNeeded(): Boolean = withContext(Dispatchers.Default) {
        try {
            val frequency = settingsRepository.settingsFlow.first().backupFrequency
            if (frequency == BackupFrequency.OFF) {
                Log.d(TAG, "Automatic backups are disabled.")
                return@withContext false
            }
            // Derive "last backup" from the newest backup on disk rather than a stored timestamp:
            // stateless, and deleting all backups correctly triggers a fresh one. Elapsed time is
            // measured in whole calendar days (toStartOfDay), matching DatabaseBackupManager.
            val lastBackup = listBackups().firstOrNull()?.lastModified ?: 0L
            val due = lastBackup == 0L ||
                (toStartOfDay(currentTimeMillis()) - toStartOfDay(lastBackup)) / MILLIS_PER_DAY >= frequency.intervalDays
            if (due) performManualBackup().isSuccess else false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error during automatic backup check", e)
            false
        }
    }

    /** Prune oldest backups beyond the user's "keep last N" retention (null = keep everything). */
    private suspend fun applyRetentionPolicy() {
        val keepCount = settingsRepository.settingsFlow.first().backupRetention.keepCount ?: return
        if (keepCount <= 0) return
        val backups = listBackups() // newest first
        if (backups.size <= keepCount) return
        val toDelete = backups.drop(keepCount).map { it.uri }
        Log.d(TAG, "Retention (keep $keepCount): pruning ${toDelete.size} old backups")
        deleteBackups(toDelete)
    }

    private fun appleDirectory(directory: NSSearchPathDirectory): String =
        NSSearchPathForDirectoriesInDomains(directory, NSUserDomainMask, true).first() as String

    private companion object {
        const val TAG = "IosBackupManager"
        const val BACKUP_PREFIX = "ShareCare_Backup"
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    }
}
