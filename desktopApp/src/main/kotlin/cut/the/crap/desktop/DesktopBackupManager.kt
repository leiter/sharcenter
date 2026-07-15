package cut.the.crap.desktop

import cut.the.crap.data.backup.BackupInfo
import cut.the.crap.data.backup.BackupManager
import cut.the.crap.platform.PlatformUri

/**
 * Placeholder desktop [BackupManager]. WP8's goal is a running desktop app; a real file-based
 * backup/restore on desktop is Decision C (feature parity) and is deferred.
 *
 * It exists so the Koin graph resolves [cut.the.crap.ui.content.settings.BackupViewModel] and the
 * settings screen renders. Everything degrades safely: no backups are listed, the daily check is a
 * no-op, and the manual actions report an unsupported failure rather than throwing.
 */
class DesktopBackupManager : BackupManager {

    override suspend fun listBackups(): List<BackupInfo> = emptyList()

    override suspend fun deleteBackups(uris: List<PlatformUri>): Result<Int> = Result.success(0)

    override suspend fun performManualBackup(): Result<String> =
        Result.failure(UnsupportedOperationException("Backup is not available on desktop yet."))

    override suspend fun restoreFromBackup(uri: PlatformUri): Result<String> =
        Result.failure(UnsupportedOperationException("Restore is not available on desktop yet."))

    override suspend fun performDailyBackupIfNeeded(): Boolean = false
}
