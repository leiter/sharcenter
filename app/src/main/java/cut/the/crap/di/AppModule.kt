package cut.the.crap.di

import android.content.Context
import androidx.room.Room
import cut.the.crap.data.db.AppDatabase
import cut.the.crap.data.db.ContentItemDao
import cut.the.crap.data.db.KeywordDao
import cut.the.crap.data.db.ContentLinkDao
import cut.the.crap.data.domain.ContentItemRepository
import cut.the.crap.data.domain.ContentItemRepositoryImpl
import cut.the.crap.data.domain.KeywordRepository
import cut.the.crap.data.domain.KeywordRepositoryImpl
import cut.the.crap.data.domain.ContentLinkRepository
import cut.the.crap.data.domain.ContentLinkRepositoryImpl
import cut.the.crap.tools.MIGRATION_1_2
import cut.the.crap.tools.MIGRATION_2_3
import cut.the.crap.tools.MIGRATION_3_4
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "app_database"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }


    @Provides
    fun provideContentLinkDao(database: AppDatabase): ContentLinkDao {
        return database.contentLinkDao()
    }

    @Provides
    @Singleton
    fun provideContentLinkRepository(contentLinkDao: ContentLinkDao): ContentLinkRepository {
        return ContentLinkRepositoryImpl(contentLinkDao)
    }

    @Provides
    fun provideKeywordDao(database: AppDatabase): KeywordDao {
        return database.keywordDao()
    }

    @Provides
    @Singleton
    fun provideKeywordRepository(keywordDao: KeywordDao): KeywordRepository {
        return KeywordRepositoryImpl(keywordDao)
    }

    @Provides
    fun provideContentItemDao(database: AppDatabase): ContentItemDao {
        return database.contentItemDao()
    }

    @Provides
    @Singleton
    fun provideContentItemRepository(contentItemDao: ContentItemDao): ContentItemRepository {
        return ContentItemRepositoryImpl(contentItemDao)
    }
}