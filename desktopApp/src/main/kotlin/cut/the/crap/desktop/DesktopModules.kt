package cut.the.crap.desktop

import app.cash.sqldelight.db.SqlDriver
import cut.the.crap.data.backup.BackupManager
import cut.the.crap.data.db.ContentItemDao
import cut.the.crap.data.db.ContentLinkDao
import cut.the.crap.data.db.DATABASE_NAME
import cut.the.crap.data.db.KeywordDao
import cut.the.crap.data.db.SqlDelightContentItemDao
import cut.the.crap.data.db.SqlDelightContentLinkDao
import cut.the.crap.data.db.SqlDelightKeywordDao
import cut.the.crap.data.db.SqlDelightSubjectDao
import cut.the.crap.data.db.SubjectDao
import cut.the.crap.data.db.createDatabase
import cut.the.crap.data.db.createDriver
import cut.the.crap.data.db.sql.ShareDatabase
import cut.the.crap.data.domain.ContentItemRepository
import cut.the.crap.data.domain.ContentItemRepositoryImpl
import cut.the.crap.data.domain.ContentLinkRepository
import cut.the.crap.data.domain.ContentLinkRepositoryImpl
import cut.the.crap.data.domain.KeywordRepository
import cut.the.crap.data.domain.KeywordRepositoryImpl
import cut.the.crap.data.domain.SubjectRepository
import cut.the.crap.data.domain.SubjectRepositoryImpl
import cut.the.crap.platform.AppRestarter
import cut.the.crap.platform.Clipboard
import cut.the.crap.platform.DesktopAppRestarter
import cut.the.crap.platform.DesktopClipboard
import cut.the.crap.platform.DesktopFileAccess
import cut.the.crap.platform.DesktopLoginFlow
import cut.the.crap.platform.CryptoProvider
import cut.the.crap.platform.DesktopIdentityKeyStore
import cut.the.crap.platform.DesktopNotifier
import cut.the.crap.platform.DesktopSharer
import cut.the.crap.platform.DesktopUrlOpener
import cut.the.crap.platform.FileAccess
import cut.the.crap.platform.LoginFlow
import cut.the.crap.platform.IdentityKeyStore
import cut.the.crap.platform.JvmCryptoProvider
import cut.the.crap.platform.Notifier
import cut.the.crap.platform.Sharer
import cut.the.crap.platform.UrlOpener
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

/**
 * The per-OS application data directory — the desktop equivalent of Android's `filesDir`, where the
 * database and preferences live. macOS/Windows use their conventional locations; Linux follows the
 * XDG spec, falling back to `~/.local/share`.
 *
 * (The preferences store computes the same base independently in `PreferencesStore.desktop`, since
 * a top-level `expect fun` there cannot take a Koin scope; keep the two in sync.)
 */
private const val APP_DIR = "ShareCare"

private fun appDataDirectory(): File {
    val home = System.getProperty("user.home")
    val os = System.getProperty("os.name").lowercase()
    val base = when {
        os.contains("mac") -> "$home/Library/Application Support/$APP_DIR"
        os.contains("win") -> (System.getenv("APPDATA") ?: "$home/AppData/Roaming") + "/$APP_DIR"
        else -> (System.getenv("XDG_DATA_HOME") ?: "$home/.local/share") + "/$APP_DIR"
    }
    return File(base)
}

/**
 * Desktop bindings for the platform seams declared in `:shared/commonMain/platform`. The desktop
 * analogue of `:app`'s `platformModule`.
 *
 * [DesktopSharer] wraps the clipboard + notifier because there is no share sheet on desktop.
 * There is no `YouTubeMetadataBackfiller` here — it is an Android-only one-time migration.
 * [BackupManager] is the deferred [DesktopBackupManager] placeholder.
 */
val desktopPlatformModule = module {
    single<Notifier> { DesktopNotifier() }
    single<FileAccess> { DesktopFileAccess() }
    single<Clipboard> { DesktopClipboard() }
    single<UrlOpener> { DesktopUrlOpener() }
    single<LoginFlow> { DesktopLoginFlow() }
    single<Sharer> { DesktopSharer(clipboard = get(), notifier = get()) }
    single<AppRestarter> { DesktopAppRestarter() }
    single<BackupManager> { DesktopBackupManager() }

    // Identity key material. Same Bouncy Castle provider as Android (jvmShared source set); the
    // seed is a chmod-600 file beside the preferences, never inside the app database.
    single<CryptoProvider> { JvmCryptoProvider() }
    single<IdentityKeyStore> { DesktopIdentityKeyStore() }
}

/**
 * Database + DAO + local-repository bindings — the desktop analogue of `:app`'s `databaseModule`,
 * differing only in the driver (JDBC instead of Android) and the absence of the Android
 * `DatabaseBackupManager` (see [desktopPlatformModule]).
 */
val desktopDatabaseModule = module {
    single<SqlDriver> { createDriver(File(appDataDirectory(), DATABASE_NAME)) }
    single { createDatabase(get()) }

    single<ContentLinkDao> { SqlDelightContentLinkDao(get<ShareDatabase>().contentLinkQueries, Dispatchers.IO) }
    single<KeywordDao> { SqlDelightKeywordDao(get<ShareDatabase>().keywordQueries, Dispatchers.IO) }
    single<ContentItemDao> { SqlDelightContentItemDao(get<ShareDatabase>().contentItemQueries, Dispatchers.IO) }
    single<SubjectDao> { SqlDelightSubjectDao(get<ShareDatabase>().subjectQueries, Dispatchers.IO) }

    // Lazy handle so injecting a consumer doesn't eagerly open the database.
    single<Lazy<SqlDriver>> { lazy { get<SqlDriver>() } }

    singleOf(::ContentLinkRepositoryImpl) bind ContentLinkRepository::class
    singleOf(::KeywordRepositoryImpl) bind KeywordRepository::class
    singleOf(::ContentItemRepositoryImpl) bind ContentItemRepository::class
    singleOf(::SubjectRepositoryImpl) bind SubjectRepository::class
}
