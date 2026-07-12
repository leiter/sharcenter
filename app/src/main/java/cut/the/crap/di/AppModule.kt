package cut.the.crap.di

import app.cash.sqldelight.db.SqlDriver
import cut.the.crap.data.backup.DatabaseBackupManager
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
import kotlinx.coroutines.Dispatchers
import cut.the.crap.data.domain.ContentItemRepository
import cut.the.crap.data.domain.ContentItemRepositoryImpl
import cut.the.crap.data.domain.ContentLinkRepository
import cut.the.crap.data.domain.ContentLinkRepositoryImpl
import cut.the.crap.data.domain.KeywordRepository
import cut.the.crap.data.domain.KeywordRepositoryImpl
import cut.the.crap.data.domain.SubjectRepository
import cut.the.crap.data.domain.SubjectRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Database + DAO + local-repository bindings.
 *
 * The store is SQLDelight (was Room); the DAO contracts are unchanged, so the
 * repositories above them are untouched. `single` mirrors the original `@Singleton` scoping.
 */
val databaseModule = module {
    single<SqlDriver> { createDriver(androidContext()) }
    single { createDatabase(get()) }

    // The DAOs live in :shared/commonMain, where Dispatchers.IO doesn't exist — the
    // platform supplies the IO dispatcher here.
    single<ContentLinkDao> { SqlDelightContentLinkDao(get<ShareDatabase>().contentLinkQueries, Dispatchers.IO) }
    single<KeywordDao> { SqlDelightKeywordDao(get<ShareDatabase>().keywordQueries, Dispatchers.IO) }
    single<ContentItemDao> { SqlDelightContentItemDao(get<ShareDatabase>().contentItemQueries, Dispatchers.IO) }
    single<SubjectDao> { SqlDelightSubjectDao(get<ShareDatabase>().subjectQueries, Dispatchers.IO) }

    // Lazy handle so injecting the backup manager doesn't eagerly open the database.
    single<Lazy<SqlDriver>> { lazy { get<SqlDriver>() } }

    singleOf(::ContentLinkRepositoryImpl) bind ContentLinkRepository::class
    singleOf(::KeywordRepositoryImpl) bind KeywordRepository::class
    singleOf(::ContentItemRepositoryImpl) bind ContentItemRepository::class
    singleOf(::SubjectRepositoryImpl) bind SubjectRepository::class

    singleOf(::DatabaseBackupManager)
}
