package cut.the.crap.di

import androidx.room.Room
import cut.the.crap.data.backup.DatabaseBackupManager
import cut.the.crap.data.db.AppDatabase
import cut.the.crap.data.domain.ContentItemRepository
import cut.the.crap.data.domain.ContentItemRepositoryImpl
import cut.the.crap.data.domain.ContentLinkRepository
import cut.the.crap.data.domain.ContentLinkRepositoryImpl
import cut.the.crap.data.domain.KeywordRepository
import cut.the.crap.data.domain.KeywordRepositoryImpl
import cut.the.crap.data.domain.SubjectRepository
import cut.the.crap.data.domain.SubjectRepositoryImpl
import cut.the.crap.tools.MIGRATION_1_2
import cut.the.crap.tools.MIGRATION_2_3
import cut.the.crap.tools.MIGRATION_3_4
import cut.the.crap.tools.MIGRATION_4_5
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Database + DAO + local-repository bindings (formerly the Hilt `AppModule`).
 * `single` mirrors the previous `@Singleton` scoping.
 */
val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "app_database"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }

    single { get<AppDatabase>().contentLinkDao() }
    single { get<AppDatabase>().keywordDao() }
    single { get<AppDatabase>().contentItemDao() }
    single { get<AppDatabase>().subjectDao() }

    // Lazy handle so injecting the backup manager doesn't eagerly open the database.
    single<Lazy<AppDatabase>> { lazy { get<AppDatabase>() } }

    singleOf(::ContentLinkRepositoryImpl) bind ContentLinkRepository::class
    singleOf(::KeywordRepositoryImpl) bind KeywordRepository::class
    singleOf(::ContentItemRepositoryImpl) bind ContentItemRepository::class
    singleOf(::SubjectRepositoryImpl) bind SubjectRepository::class

    singleOf(::DatabaseBackupManager)
}
