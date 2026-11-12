package dev.erolit.reader.core.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stories",
    indices = [Index("slug"), Index("authorId"), Index("seriesId")],
)
data class StoryEntity(
    @PrimaryKey val id: Long,
    val slug: String?,
    val title: String,
    val authorId: Long?,
    val authorName: String?,
    val authorPic: String?,
    val categoryId: Int?,
    val categoryName: String?,
    val description: String?,
    val words: Int?,
    val readingMinutes: Int?,
    val rating: Double?,
    val ratingCount: Int?,
    val publishedAt: String?,
    val type: String?,
    val pagesCount: Int?,
    val seriesId: Long?,
    val seriesTitle: String?,
    val tags: String?,
    val updatedAt: Long,
)

@Entity(
    tableName = "story_pages",
    primaryKeys = ["storyId", "page"],
    foreignKeys = [
        ForeignKey(
            entity = StoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["storyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("storyId")],
)
data class StoryPageEntity(
    val storyId: Long,
    val page: Int,
    val text: String,
    val fetchedAt: Long,
)

@Entity(tableName = "progress", indices = [Index("updatedAt")])
data class ProgressEntity(
    @PrimaryKey val storyId: Long,
    val page: Int,
    val scrollFraction: Float,
    val percent: Float,
    val updatedAt: Long,
)

@Entity(tableName = "search_history", primaryKeys = ["query", "type"])
data class SearchHistoryEntity(
    val query: String,
    val type: String,
    val lastUsedAt: Long,
)

/**
 * A story the reader saved to the on-device list. It is deliberately not tied
 * to an account: it survives sign-out and is never sent to the server.
 */
@Entity(tableName = "local_saved", indices = [Index("savedAt")])
data class LocalSavedEntity(
    @PrimaryKey val storyId: Long,
    val slug: String?,
    val title: String,
    val authorId: Long?,
    val authorName: String?,
    val categoryId: Int?,
    val categoryName: String?,
    val description: String?,
    val words: Int?,
    val readingMinutes: Int?,
    val rating: Double?,
    val ratingCount: Int?,
    val publishedAt: String?,
    val type: String?,
    val seriesId: Long?,
    val seriesTitle: String?,
    val tags: String?,
    val savedAt: Long,
)

/**
 * A series the reader saved for local browsing. The chapter metadata lives
 * in [LocalSeriesWorkEntity], so the series screen can render its table of
 * contents without a network request.
 */
@Entity(tableName = "local_series", indices = [Index("savedAt")])
data class LocalSeriesEntity(
    @PrimaryKey val seriesId: Long,
    val title: String,
    val authorId: Long?,
    val authorName: String?,
    val description: String?,
    val state: String?,
    val workCount: Int,
    val savedAt: Long,
)

@Entity(
    tableName = "local_series_works",
    primaryKeys = ["seriesId", "storyId"],
    foreignKeys = [
        ForeignKey(
            entity = LocalSeriesEntity::class,
            parentColumns = ["seriesId"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("seriesId")],
)
data class LocalSeriesWorkEntity(
    val seriesId: Long,
    val storyId: Long,
    val position: Int,
    val slug: String?,
    val title: String,
    val authorId: Long?,
    val authorName: String?,
    val authorPic: String?,
    val categoryId: Int?,
    val categoryName: String?,
    val description: String?,
    val words: Int?,
    val readingMinutes: Int?,
    val rating: Double?,
    val ratingCount: Int?,
    val publishedAt: String?,
    val type: String?,
    val tags: String?,
)

/**
 * One row per story the reader opened, with the moment it was last opened.
 * Metadata is copied in so History still renders without a network request.
 */
@Entity(tableName = "history", indices = [Index("openedAt")])
data class HistoryEntity(
    @PrimaryKey val storyId: Long,
    val slug: String?,
    val title: String,
    val authorId: Long?,
    val authorName: String?,
    val authorPic: String?,
    val categoryId: Int?,
    val categoryName: String?,
    val description: String?,
    val words: Int?,
    val readingMinutes: Int?,
    val rating: Double?,
    val ratingCount: Int?,
    val publishedAt: String?,
    val type: String?,
    val seriesId: Long?,
    val seriesTitle: String?,
    val tags: String?,
    val openedAt: Long,
)

/** An author the reader saved on this device. Never tied to an account. */
@Entity(tableName = "local_authors", indices = [Index("savedAt")])
data class LocalAuthorEntity(
    @PrimaryKey val authorId: Long,
    val username: String,
    val userpic: String?,
    val bio: String?,
    val followers: Int?,
    val storiesCount: Int?,
    val seriesCount: Int?,
    val savedAt: Long,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val shortName: String?,
    val description: String?,
    val pageUrl: String?,
    val type: String?,
    val submissionCount: Int?,
    val fetchedAt: Long,
)
