package cut.the.crap.data.backup

import cut.the.crap.platform.PlatformUri

/**
 * Database backup/restore is not offered on iOS v1 — the Android implementation is MediaStore/SAF
 * specific, and a proper iOS story (Files export, iCloud) is out of scope for the first cut. The
 * capability is declared inert rather than faked: listing yields nothing, auto-backup never runs,
 * and manual backup/restore return a failure the UI can surface.
 */
class IosBackupManager : BackupManager {

    override suspend fun listBackups(): List<BackupInfo> = emptyList()

    override suspend fun deleteBackups(uris: List<PlatformUri>): Result<Int> = Result.success(0)

    override suspend fun performManualBackup(): Result<String> =
        Result.failure(UnsupportedOperationException("Database backup is not available on iOS yet."))

    override suspend fun restoreFromBackup(uri: PlatformUri): Result<String> =
        Result.failure(UnsupportedOperationException("Database restore is not available on iOS yet."))

    override suspend fun performDailyBackupIfNeeded(): Boolean = false
}
