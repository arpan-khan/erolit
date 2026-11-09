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
