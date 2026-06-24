package cut.the.crap.data.backup

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cut.the.crap.data.db.AppDatabase
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.backupDataStore: DataStore<Preferences> by preferencesDataStore(name = "backup_preferences")

@Singleton
class DatabaseBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    // Lazy so injecting the manager doesn't eagerly open the database.
    private val database: Lazy<AppDatabase>
) {
    private object PreferencesKeys {
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
    }

    companion object {
        private const val TAG = "DatabaseBackupManager"
        private const val DATABASE_NAME = "app_database"
        private const val BACKUP_PREFIX = "ShareCare_Backup"

        // Keep in sync with the @Database(version = ...) value in AppDatabase.
        // A backup whose user_version is higher than this would require a
        // downgrade, which Room cannot do — such backups are rejected.
        private const val CURRENT_SCHEMA_VERSION = 4
    }

    /**
     * Checks if a backup is needed (first app start of the day) and performs it if necessary.
     * @return true if backup was performed, false otherwise
     */
    suspend fun performDailyBackupIfNeeded(): Boolean {
        return try {
            val lastBackupTimestamp = getLastBackupTimestamp()
            val currentTime = System.currentTimeMillis()

            if (shouldPerformBackup(lastBackupTimestamp, currentTime)) {
                Log.d(TAG, "Daily backup needed. Last backup: ${formatTimestamp(lastBackupTimestamp)}")
                performBackup(currentTime)
                true
            } else {
                Log.d(TAG, "Daily backup not needed. Last backup: ${formatTimestamp(lastBackupTimestamp)}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during daily backup check", e)
            false
        }
    }

    /**
     * Determines if a backup should be performed based on last backup timestamp.
     * Backup is needed if:
     * - Never backed up before (lastBackupTimestamp == 0)
     * - Last backup was on a different day
     */
    private fun shouldPerformBackup(lastBackupTimestamp: Long, currentTime: Long): Boolean {
        if (lastBackupTimestamp == 0L) {
            return true // Never backed up before
        }

        // Check if last backup was on a different day
        val lastBackupDate = getDateWithoutTime(lastBackupTimestamp)
        val currentDate = getDateWithoutTime(currentTime)

        return lastBackupDate < currentDate
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
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
            val formattedDate = dateFormat.format(Date(timestamp))
            val backupFileName = "${BACKUP_PREFIX}_${formattedDate}.db"

            val backupResult = copyDatabaseToDownloads(dbFile, backupFileName)

            if (backupResult.isSuccess) {
                // Update last backup timestamp
                saveLastBackupTimestamp(timestamp)
                Log.i(TAG, "Database backup successful: $backupFileName")
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
     * Forces an immediate backup regardless of the last backup time.
     * Useful for manual backup triggers.
     */
    suspend fun performManualBackup(): Result<String> {
        Log.d(TAG, "Manual backup requested")
        return performBackup(System.currentTimeMillis())
    }

    /**
     * Restores the database from a user-selected `.db` backup file (a SAF [Uri],
     * typically one of the `ShareCare_Backup_*.db` files written to Downloads).
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
    suspend fun restoreFromBackup(uri: Uri): Result<String> {
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

            // Replace the live database file. Close Room first so the file isn't held open.
            database.get().close()

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
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
