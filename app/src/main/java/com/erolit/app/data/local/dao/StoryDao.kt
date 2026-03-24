package com.erolit.app.data.local.dao

import androidx.room.*
import com.erolit.app.data.local.entity.StoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {

    @Upsert
    suspend fun upsertStory(story: StoryEntity)

    @Upsert
    suspend fun upsertStories(stories: List<StoryEntity>)

    @Query("SELECT * FROM stories WHERE slug = :slug")
    suspend fun getStoryBySlug(slug: String): StoryEntity?

    @Query("SELECT * FROM stories WHERE isSaved = 1 ORDER BY savedAt DESC")
    fun getSavedStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE lastReadAt > 0 ORDER BY lastReadAt DESC LIMIT 50")
    fun getRecentlyRead(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE isDownloaded = 1 ORDER BY savedAt DESC")
    fun getDownloadedStories(): Flow<List<StoryEntity>>

    @Query("UPDATE stories SET isSaved = :saved, savedAt = :savedAt WHERE slug = :slug")
    suspend fun updateSavedStatus(slug: String, saved: Boolean, savedAt: Long)

    @Query("UPDATE stories SET isDownloaded = :downloaded WHERE slug = :slug")
    suspend fun updateDownloadStatus(slug: String, downloaded: Boolean)

    @Query("UPDATE stories SET readingProgress = :progress, lastPageRead = :page, lastReadAt = :readAt WHERE slug = :slug")
    suspend fun updateReadingProgress(slug: String, progress: Float, page: Int, readAt: Long)

    @Query("SELECT isSaved FROM stories WHERE slug = :slug")
    suspend fun isSaved(slug: String): Boolean?

    @Query("SELECT isDownloaded FROM stories WHERE slug = :slug")
    suspend fun isDownloaded(slug: String): Boolean?

    @Query("DELETE FROM stories WHERE slug = :slug")
    suspend fun deleteStory(slug: String)
}
