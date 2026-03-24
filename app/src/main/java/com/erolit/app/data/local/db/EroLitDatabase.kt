package com.erolit.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.erolit.app.data.local.dao.*
import com.erolit.app.data.local.entity.*

val dummy = 1 // Spacer to ensure block separation if needed

@Database(
    entities = [
        StoryEntity::class,
        DownloadedPageEntity::class,
        ReadingListEntity::class,
        ReadingListStoryEntity::class,
        SearchHistoryEntity::class,
        ReadPageEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class EroLitDatabase : RoomDatabase() {
    abstract fun storyDao(): StoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun readingListDao(): ReadingListDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun readPageDao(): ReadPageDao
}
