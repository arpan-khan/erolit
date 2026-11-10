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
        LocalSeriesEntity::class,
        LocalSeriesWorkEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storyDao(): StoryDao
    abstract fun progressDao(): ProgressDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun categoryDao(): CategoryDao
    abstract fun localSavedDao(): LocalSavedDao
    abstract fun localSeriesDao(): LocalSeriesDao

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `stories` ADD COLUMN `tags` TEXT")
                db.execSQL("ALTER TABLE `local_saved` ADD COLUMN `tags` TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_series` (
                        `seriesId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `authorId` INTEGER,
                        `authorName` TEXT,
                        `description` TEXT,
                        `state` TEXT,
                        `workCount` INTEGER NOT NULL,
                        `savedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`seriesId`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_local_series_savedAt` ON `local_series` (`savedAt`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_series_works` (
                        `seriesId` INTEGER NOT NULL,
                        `storyId` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        `slug` TEXT,
                        `title` TEXT NOT NULL,
                        `authorId` INTEGER,
                        `authorName` TEXT,
                        `authorPic` TEXT,
                        `categoryId` INTEGER,
                        `categoryName` TEXT,
                        `description` TEXT,
                        `words` INTEGER,
                        `readingMinutes` INTEGER,
                        `rating` REAL,
                        `ratingCount` INTEGER,
                        `publishedAt` TEXT,
                        `type` TEXT,
                        `tags` TEXT,
                        PRIMARY KEY(`seriesId`, `storyId`),
                        FOREIGN KEY(`seriesId`) REFERENCES `local_series`(`seriesId`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_local_series_works_seriesId` " +
                        "ON `local_series_works` (`seriesId`)",
                )
            }
        }

        fun build(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "erolit.db",
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }
}
