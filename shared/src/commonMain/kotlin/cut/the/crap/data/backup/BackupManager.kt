package cut.the.crap.data.backup

import cut.the.crap.platform.PlatformUri

/**
 * A database backup file on disk.
 *
 * @property uri handle used to open or delete the file. Opaque to shared code — on Android it is
 *   a MediaStore/SAF `content://` Uri, on desktop it will be a filesystem path.
 */
data class BackupInfo(
    val uri: PlatformUri,
    val displayName: String,
    val sizeBytes: Long,
    val lastModified: Long,
)

/**
 * Creating, listing, deleting and restoring database backups.
 *
 * The implementation is deeply platform-specific (Android uses MediaStore and SAF), which is
 * exactly why it sits behind an interface: the backup *screen* only ever treats a backup's
 * [BackupInfo.uri] as an opaque identity, so none of that has to leak into the UI.
 */
interface BackupManager {

    /** Existing backups, newest first. */
    suspend fun listBackups(): List<BackupInfo>

    /** Deletes the given backups. Returns how many were actually removed. */
    suspend fun deleteBackups(uris: List<PlatformUri>): Result<Int>

    /** Backs up now, regardless of schedule. Returns the file name written. */
    suspend fun performManualBackup(): Result<String>

    /** Replaces the live database with the given backup. Returns the file name restored. */
    suspend fun restoreFromBackup(uri: PlatformUri): Result<String>

    /** Runs the daily backup if one is due. Returns true if a backup was written. */
    suspend fun performDailyBackupIfNeeded(): Boolean
}
