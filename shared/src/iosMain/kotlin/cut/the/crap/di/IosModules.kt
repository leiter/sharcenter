package cut.the.crap.di

import app.cash.sqldelight.db.SqlDriver
import cut.the.crap.data.backup.BackupManager
import cut.the.crap.data.backup.IosBackupManager
import cut.the.crap.data.db.ContentItemDao
import cut.the.crap.data.db.ContentLinkDao
import cut.the.crap.data.db.KeywordDao
import cut.the.crap.data.db.SubjectDao
import cut.the.crap.data.db.SqlDelightContentItemDao
import cut.the.crap.data.db.SqlDelightContentLinkDao
import cut.the.crap.data.db.SqlDelightKeywordDao
import cut.the.crap.data.db.SqlDelightSubjectDao
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
import cut.the.crap.platform.FileAccess
import cut.the.crap.platform.IosAppRestarter
import cut.the.crap.platform.IosClipboard
import cut.the.crap.platform.IosFileAccess
import cut.the.crap.platform.IosLoginFlow
import cut.the.crap.platform.IosNotifier
import cut.the.crap.platform.IosSharer
import cut.the.crap.platform.IosUrlOpener
import cut.the.crap.platform.LoginFlow
import cut.the.crap.platform.Notifier
import cut.the.crap.platform.Sharer
import cut.the.crap.platform.UrlOpener
import cut.the.crap.tools.defaultIoDispatcher
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * iOS bindings for the platform seams — the counterpart to :app's `platformModule`. The Xcode app
 * (WP-iOS-6) starts Koin with this plus [iosDatabaseModule], the common `viewModelModule`, and the
 * network/repository modules once the config seam lands (WP-iOS-5).
 */
val iosPlatformModule = module {
    single<Notifier> { IosNotifier() }
    single<FileAccess> { IosFileAccess() }
    single<Clipboard> { IosClipboard() }
    single<UrlOpener> { IosUrlOpener() }
    single<Sharer> { IosSharer() }
    single<LoginFlow> { IosLoginFlow() }
    single<AppRestarter> { IosAppRestarter() }
}

/**
 * iOS database + DAO + local-repository bindings — the counterpart to :app's `databaseModule`. Uses
 * the native SQLite driver and [defaultIoDispatcher] (the seam that stands in for `Dispatchers.IO`,
 * which does not exist in commonMain).
 */
val iosDatabaseModule = module {
    single<SqlDriver> { createDriver() }
    single { createDatabase(get()) }

    single<ContentLinkDao> { SqlDelightContentLinkDao(get<ShareDatabase>().contentLinkQueries, defaultIoDispatcher) }
    single<KeywordDao> { SqlDelightKeywordDao(get<ShareDatabase>().keywordQueries, defaultIoDispatcher) }
    single<ContentItemDao> { SqlDelightContentItemDao(get<ShareDatabase>().contentItemQueries, defaultIoDispatcher) }
    single<SubjectDao> { SqlDelightSubjectDao(get<ShareDatabase>().subjectQueries, defaultIoDispatcher) }

    singleOf(::ContentLinkRepositoryImpl) bind ContentLinkRepository::class
    singleOf(::KeywordRepositoryImpl) bind KeywordRepository::class
    singleOf(::ContentItemRepositoryImpl) bind ContentItemRepository::class
    singleOf(::SubjectRepositoryImpl) bind SubjectRepository::class

    single<BackupManager> { IosBackupManager(get(), get()) }
}
