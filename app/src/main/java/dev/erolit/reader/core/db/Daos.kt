package dev.erolit.reader.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class RecentRow(
    val storyId: Long,
    val page: Int,
    val scrollFraction: Float,
    val percent: Float,
    val updatedAt: Long,
    val title: String?,
    val authorName: String?,
    val slug: String?,
    val type: String?,
)

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun find(id: Long): StoryEntity?

    @Upsert
    suspend fun upsert(story: StoryEntity)

    @Query("SELECT * FROM story_pages WHERE storyId = :storyId AND page = :page")
    suspend fun findPage(storyId: Long, page: Int): StoryPageEntity?

    @Upsert
    suspend fun upsertPage(page: StoryPageEntity)

    @Query("DELETE FROM story_pages WHERE storyId = :storyId")
    suspend fun clearPagesFor(storyId: Long)

    @Query("DELETE FROM story_pages")
    suspend fun clearAllPages()

    @Query("SELECT COALESCE(SUM(LENGTH(text)), 0) FROM story_pages")
    suspend fun cachedCharacters(): Long

    @Query("SELECT COUNT(*) FROM story_pages")
    suspend fun cachedPageCount(): Int
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE storyId = :storyId")
    suspend fun find(storyId: Long): ProgressEntity?

    @Upsert
    suspend fun upsert(progress: ProgressEntity)

    @Upsert
    suspend fun upsertAll(items: List<ProgressEntity>)

    @Query("DELETE FROM progress WHERE storyId = :storyId")
    suspend fun delete(storyId: Long)

    @Query("DELETE FROM progress WHERE storyId IN (:ids)")
    suspend fun deleteFor(ids: List<Long>)

    @Query("SELECT * FROM progress WHERE storyId IN (:ids)")
    suspend fun findFor(ids: List<Long>): List<ProgressEntity>

    @Query("SELECT * FROM progress")
    fun observeAll(): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress")
    suspend fun all(): List<ProgressEntity>

    @Query(
        """
        SELECT p.storyId AS storyId, p.page AS page, p.scrollFraction AS scrollFraction,
               p.percent AS percent, p.updatedAt AS updatedAt,
               s.title AS title, s.authorName AS authorName, s.slug AS slug, s.type AS type
        FROM progress p LEFT JOIN stories s ON s.id = p.storyId
        ORDER BY p.updatedAt DESC
        LIMIT 20
        """,
    )
    fun observeRecent(): Flow<List<RecentRow>>

    @Query("DELETE FROM progress")
    suspend fun clear()
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY lastUsedAt DESC LIMIT 20")
    fun observeRecent(): Flow<List<SearchHistoryEntity>>

    @Upsert
    suspend fun upsert(item: SearchHistoryEntity)

    @Query("DELETE FROM search_history")
    suspend fun clear()
}

@Dao
interface LocalSavedDao {
    @Query("SELECT * FROM local_saved ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<LocalSavedEntity>>

    @Query("SELECT * FROM local_saved")
    suspend fun all(): List<LocalSavedEntity>

    @Query("SELECT * FROM local_saved WHERE storyId = :storyId")
    suspend fun find(storyId: Long): LocalSavedEntity?

    @Upsert
    suspend fun upsert(item: LocalSavedEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LocalSavedEntity>)

    @Query("DELETE FROM local_saved WHERE storyId = :storyId")
    suspend fun delete(storyId: Long)

    @Query("DELETE FROM local_saved")
    suspend fun clear()
}

@Dao
interface LocalSeriesDao {
    @Query("SELECT * FROM local_series ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<LocalSeriesEntity>>

    @Query("SELECT * FROM local_series")
    suspend fun allSeries(): List<LocalSeriesEntity>

    @Query("SELECT * FROM local_series_works")
    suspend fun allWorks(): List<LocalSeriesWorkEntity>

    @Query("SELECT * FROM local_series WHERE seriesId = :seriesId")
    suspend fun find(seriesId: Long): LocalSeriesEntity?

    @Upsert
    suspend fun upsertSeries(series: LocalSeriesEntity)

    @Upsert
    suspend fun upsertSeriesList(series: List<LocalSeriesEntity>)

    @Query("SELECT * FROM local_series_works WHERE seriesId = :seriesId ORDER BY position")
    suspend fun works(seriesId: Long): List<LocalSeriesWorkEntity>

    @Query("SELECT * FROM local_series_works ORDER BY seriesId, position")
    fun observeAllWorks(): Flow<List<LocalSeriesWorkEntity>>

    @Query("DELETE FROM local_series_works WHERE seriesId = :seriesId")
    suspend fun clearWorks(seriesId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorks(items: List<LocalSeriesWorkEntity>)

    @Query("DELETE FROM local_series WHERE seriesId = :seriesId")
    suspend fun delete(seriesId: Long)

    @Query("DELETE FROM local_series")
    suspend fun clearAll()
}

@Dao
interface LocalAuthorDao {
    @Query("SELECT * FROM local_authors ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<LocalAuthorEntity>>

    @Query("SELECT * FROM local_authors")
    suspend fun all(): List<LocalAuthorEntity>

    @Query("SELECT * FROM local_authors WHERE authorId = :authorId")
    suspend fun find(authorId: Long): LocalAuthorEntity?

    @Upsert
    suspend fun upsert(item: LocalAuthorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LocalAuthorEntity>)

    @Query("DELETE FROM local_authors WHERE authorId = :authorId")
    suspend fun delete(authorId: Long)

    @Query("DELETE FROM local_authors")
    suspend fun clear()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY openedAt DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history")
    suspend fun all(): List<HistoryEntity>

    @Upsert
    suspend fun upsert(item: HistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HistoryEntity>)

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name")
    suspend fun all(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun clear()
}
