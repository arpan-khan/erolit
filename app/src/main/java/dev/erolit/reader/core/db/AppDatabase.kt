package dev.erolit.reader.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        StoryEntity::class,
        StoryPageEntity::class,
        ProgressEntity::class,
        SearchHistoryEntity::class,
        CategoryEntity::class,
        LocalSavedEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storyDao(): StoryDao
    abstract fun progressDao(): ProgressDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun categoryDao(): CategoryDao
    abstract fun localSavedDao(): LocalSavedDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_saved` (
                        `storyId` INTEGER NOT NULL,
                        `slug` TEXT,
                        `title` TEXT NOT NULL,
                        `authorId` INTEGER,
                        `authorName` TEXT,
                        `categoryId` INTEGER,
                        `categoryName` TEXT,
                        `description` TEXT,
                        `words` INTEGER,
                        `readingMinutes` INTEGER,
                        `rating` REAL,
                        `ratingCount` INTEGER,
                        `publishedAt` TEXT,
                        `type` TEXT,
                        `seriesId` INTEGER,
                        `seriesTitle` TEXT,
                        `savedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`storyId`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_local_saved_savedAt` ON `local_saved` (`savedAt`)")
            }
        }

        fun build(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "erolit.db",
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }
}
