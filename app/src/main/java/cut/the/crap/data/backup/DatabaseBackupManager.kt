package cut.the.crap.data.backup

import cut.the.crap.platform.toPlatformUri

import cut.the.crap.platform.toAndroidUri

import cut.the.crap.platform.PlatformUri

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import cut.the.crap.platform.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.cash.sqldelight.db.SqlDriver
import cut.the.crap.data.preferences.SettingsRepository
import cut.the.crap.ui.content.settings.BackupFrequency
import kotlinx.coroutines.flow.first
import cut.the.crap.tools.formatTimestampForFileName
import cut.the.crap.tools.formatTimestampIsoLike
import java.io.File
import java.io.FileInputStream
import java.io.IOException

private val Context.backupDataStore: DataStore<Preferences> by preferencesDataStore(name = "backup_preferences")

/**
 * Android implementation of [BackupManager], backed by MediaStore (API 29+) and SAF.
 *
 * All the `content://` Uri handling stays inside this class: the interface speaks [PlatformUri],
 * so the UI never learns what a ContentResolver is.
 */
class DatabaseBackupManager constructor(
    private val context: Context,
    // Lazy so injecting the manager doesn't eagerly open the database.
    private val database: Lazy<SqlDriver>,
    private val settingsRepository: SettingsRepository
) : BackupManager {
    private object PreferencesKeys {
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
    }

    companion object {
        private const val TAG = "DatabaseBackupManager"
        private const val DATABASE_NAME = "app_database"
        private const val BACKUP_PREFIX = "ShareCenter_Backup"
        private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

        // Keep in sync with the SQLDelight schema version (see DatabaseFactory).
        // A backup whose user_version is higher than this would require a
        // downgrade, which we cannot do — such backups are rejected.
        private const val CURRENT_SCHEMA_VERSION = cut.the.crap.data.db.CURRENT_SCHEMA_VERSION
    }

    /**
     * Checks if an automatic backup is due (based on the user's configured
     * [BackupFrequency]) and performs it if necessary.
     * @return true if backup was performed, false otherwise
     */
    override suspend fun performDailyBackupIfNeeded(): Boolean {
        return try {
            val frequency = settingsRepository.settingsFlow.first().backupFrequency
            if (frequency == BackupFrequency.OFF) {
                Log.d(TAG, "Automatic backups are disabled.")
                return false
            }

            val lastBackupTimestamp = getLastBackupTimestamp()
            val currentTime = System.currentTimeMillis()

            if (shouldPerformBackup(lastBackupTimestamp, currentTime, frequency)) {
                Log.d(TAG, "Backup needed ($frequency). Last backup: ${formatTimestamp(lastBackupTimestamp)}")
                performBackup(currentTime)
                true
            } else {
                Log.d(TAG, "Backup not needed ($frequency). Last backup: ${formatTimestamp(lastBackupTimestamp)}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during automatic backup check", e)
            false
        }
    }

    /**
     * Determines if a backup should be performed based on the last backup timestamp
     * and the configured [frequency]. Backup is needed if:
     * - Never backed up before (lastBackupTimestamp == 0), or
     * - At least [BackupFrequency.intervalDays] calendar days have elapsed since the
     *   last backup.
     */
    private fun shouldPerformBackup(
        lastBackupTimestamp: Long,
        currentTime: Long,
        frequency: BackupFrequency
    ): Boolean {
        if (lastBackupTimestamp == 0L) {
            return true // Never backed up before
        }

        val elapsedDays =
            (getDateWithoutTime(currentTime) - getDateWithoutTime(lastBackupTimestamp)) / MILLIS_PER_DAY
        return elapsedDays >= frequency.intervalDays
    }

    /**
     * Strips time component from timestamp, leaving only the date.
     */
    private fun getDateWithoutTime(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Performs the actual database backup to the Downloads folder.
     */
    private suspend fun performBackup(timestamp: Long): Result<String> {
        return try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)

            if (!dbFile.exists()) {
                Log.w(TAG, "Database file does not exist: ${dbFile.absolutePath}")
                return Result.failure(IOException("Database file not found"))
            }

            // Generate filename with timestamp
            val formattedDate = formatTimestampForFileName(timestamp)
            val backupFileName = "${BACKUP_PREFIX}_${formattedDate}.db"

            val backupResult = copyDatabaseToDownloads(dbFile, backupFileName)

            if (backupResult.isSuccess) {
                // Update last backup timestamp
                saveLastBackupTimestamp(timestamp)
                Log.i(TAG, "Database backup successful: $backupFileName")
                // Prune old backups according to the configured retention policy.
                applyRetentionPolicy()
                Result.success(backupFileName)
            } else {
                Log.e(TAG, "Database backup failed", backupResult.exceptionOrNull())
                backupResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing backup", e)
            Result.failure(e)
        }
    }

    /**
     * Copies database file to Downloads folder using MediaStore API (Android 10+)
     * or direct file copy (Android 9 and below).
     */
    private fun copyDatabaseToDownloads(sourceFile: File, fileName: String): Result<String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - Use MediaStore API
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        FileInputStream(sourceFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Log.d(TAG, "Backup saved to Downloads: $fileName")
                    Result.success(fileName)
                } ?: Result.failure(IOException("Failed to create file in Downloads"))
            } else {
                // Android 9 and below - Direct file copy
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                val destinationFile = File(downloadsDir, fileName)

                FileInputStream(sourceFile).use { inputStream ->
                    destinationFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                Log.d(TAG, "Backup saved to: ${destinationFile.absolutePath}")
                Result.success(destinationFile.absolutePath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying database to Downloads", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves the last backup timestamp from DataStore.
     */
    private suspend fun getLastBackupTimestamp(): Long {
        return context.backupDataStore.data.first()[PreferencesKeys.LAST_BACKUP_TIMESTAMP] ?: 0L
    }

    /**
     * Saves the last backup timestamp to DataStore.
     */
    private suspend fun saveLastBackupTimestamp(timestamp: Long) {
        context.backupDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }

    /**
     * Lists all existing `ShareCenter_Backup_*.db` files in Downloads, newest first.
     * Returns an empty list if none exist or the query fails.
     */
    override suspend fun listBackups(): List<BackupInfo> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                queryMediaStoreBackups()
            } else {
                listLegacyBackups()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing backups", e)
            emptyList()
        }
    }

    /**
     * Deletes the given backup files.
     *
     * Note: on Android 10+ the app can delete MediaStore files it created without a
     * prompt. Deleting files created by another app would throw a
     * `RecoverableSecurityException` requiring user consent — that is not handled here
     * because all `ShareCenter_Backup_*.db` files are created by this app.
     *
     * @return [Result.success] with the number of files actually deleted, or
     *         [Result.failure] if the operation could not be carried out at all.
     */
    override suspend fun deleteBackups(uris: List<PlatformUri>): Result<Int> {
        val uris = uris.map { it.toAndroidUri() }
        return try {
            var deleted = 0
            for (uri in uris) {
                try {
                    val removed = if (uri.scheme == "file") {
                        if (uri.path?.let { File(it).delete() } == true) 1 else 0
                    } else {
                        context.contentResolver.delete(uri, null, null)
                    }
                    if (removed > 0) deleted++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete backup: $uri", e)
                }
            }
            Log.i(TAG, "Deleted $deleted of ${uris.size} backups")
            Result.success(deleted)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting backups", e)
            Result.failure(e)
        }
    }

    /**
     * Applies the user's retention policy by deleting the oldest backups beyond the
     * configured "keep last N" count. A null count means keep everything.
     */
    private suspend fun applyRetentionPolicy() {
        val keepCount = settingsRepository.settingsFlow.first().backupRetention.keepCount ?: return
        if (keepCount <= 0) return

        val backups = listBackups() // newest first
        if (backups.size <= keepCount) return

        val toDelete = backups.drop(keepCount).map { it.uri }
        Log.d(TAG, "Retention policy (keep $keepCount): pruning ${toDelete.size} old backups")
        deleteBackups(toDelete)
    }

    /**
     * Queries MediaStore Downloads for backup files (Android 10+).
     */
    private fun queryMediaStoreBackups(): List<BackupInfo> {
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("$BACKUP_PREFIX%")
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        val backups = mutableListOf<BackupInfo>()
        context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                backups += BackupInfo(
                    uri = ContentUris.withAppendedId(collection, id).toPlatformUri(),
                    displayName = cursor.getString(nameColumn),
                    sizeBytes = cursor.getLong(sizeColumn),
                    // DATE_MODIFIED is in seconds since epoch; convert to millis.
                    lastModified = cursor.getLong(dateColumn) * 1000
                )
            }
        }
        return backups
    }

    /**
     * Lists backup files directly from the public Downloads directory (Android 9 and below).
     */
    private fun listLegacyBackups(): List<BackupInfo> {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val files = downloadsDir.listFiles { file ->
            file.isFile && file.name.startsWith(BACKUP_PREFIX)
        } ?: return emptyList()

        return files
            .map { file ->
                BackupInfo(
                    uri = Uri.fromFile(file).toPlatformUri(),
                    displayName = file.name,
                    sizeBytes = file.length(),
                    lastModified = file.lastModified()
                )
            }
            .sortedByDescending { it.lastModified }
    }

    /**
     * Forces an immediate backup regardless of the last backup time.
     * Useful for manual backup triggers.
     */
    override suspend fun performManualBackup(): Result<String> {
        Log.d(TAG, "Manual backup requested")
        return performBackup(System.currentTimeMillis())
    }

    /**
     * Restores the database from a user-selected `.db` backup file (a SAF [Uri],
     * typically one of the `ShareCenter_Backup_*.db` files written to Downloads).
     *
     * The backup is validated as a real SQLite database with a compatible schema
     * version, then the live database is closed and its file replaced. The caller
     * MUST restart the app process afterwards so Room reopens the restored file
     * (running any forward migrations as needed).
     *
     * @return [Result.success] with the restored file's display name, or
     *         [Result.failure] if the file is missing, not a valid/compatible DB,
     *         or the copy failed (in which case the existing DB is left untouched).
     */
    override suspend fun restoreFromBackup(uri: PlatformUri): Result<String> {
        val uri = uri.toAndroidUri()
        Log.d(TAG, "Restore requested from: $uri")
        // Stage the picked file in cache so we can validate it before touching the live DB.
        val tempFile = File(context.cacheDir, "restore_candidate.db")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return Result.failure(IOException("Could not open backup file"))

            // Validate: must be a real SQLite DB with a schema version we can open.
            val validation = validateBackup(tempFile)
            validation.exceptionOrNull()?.let { return Result.failure(it) }

            // Replace the live database file. Close the driver first so the file isn't held open.
            database.value.close()

            val dbFile = context.getDatabasePath(DATABASE_NAME)
            dbFile.parentFile?.mkdirs()
            tempFile.inputStream().use { input ->
                dbFile.outputStream().use { output -> input.copyTo(output) }
            }
            // Remove WAL/SHM/journal sidecars so they can't override the restored data.
            listOf("$DATABASE_NAME-wal", "$DATABASE_NAME-shm", "$DATABASE_NAME-journal").forEach {
                File(dbFile.parentFile, it).delete()
            }

            val displayName = queryDisplayName(uri) ?: dbFile.name
            Log.i(TAG, "Database restored from $displayName. App restart required.")
            Result.success(displayName)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring database", e)
            Result.failure(e)
        } finally {
            tempFile.delete()
        }
    }

    /**
     * Verifies [file] is a valid SQLite database whose schema version is not newer
     * than the one this app build understands.
     */
    private fun validateBackup(file: File): Result<Unit> {
        if (!file.exists() || file.length() == 0L) {
            return Result.failure(IOException("Backup file is empty"))
        }
        return try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val version = db.version // SQLite user_version
                if (version > CURRENT_SCHEMA_VERSION) {
                    Result.failure(
                        IOException(
                            "Backup is from a newer app version (schema v$version); " +
                                "cannot restore into this build (schema v$CURRENT_SCHEMA_VERSION)."
                        )
                    )
                } else {
                    Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException("Selected file is not a valid database", e))
        }
    }

    /**
     * Resolves a human-readable display name for a SAF [Uri], if available.
     */
    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats a timestamp for logging purposes.
     */
    private fun formatTimestamp(timestamp: Long): String {
        return if (timestamp == 0L) {
            "Never"
        } else {
            formatTimestampIsoLike(timestamp)
        }
    }
}
