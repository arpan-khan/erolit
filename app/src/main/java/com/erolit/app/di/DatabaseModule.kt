package com.erolit.app.di

import android.content.Context
import androidx.room.Room
import com.erolit.app.data.local.dao.*
import com.erolit.app.data.local.db.EroLitDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EroLitDatabase {
        return Room.databaseBuilder(
            context,
            EroLitDatabase::class.java,
            "erolit.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideStoryDao(db: EroLitDatabase): StoryDao = db.storyDao()
    @Provides fun provideDownloadDao(db: EroLitDatabase): DownloadDao = db.downloadDao()
    @Provides fun provideReadingListDao(db: EroLitDatabase): ReadingListDao = db.readingListDao()
    @Provides fun provideSearchHistoryDao(db: EroLitDatabase): SearchHistoryDao = db.searchHistoryDao()
    @Provides fun provideReadPageDao(db: EroLitDatabase): ReadPageDao = db.readPageDao()
}
