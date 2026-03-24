package com.erolit.app.data.repository

import com.erolit.app.data.local.dao.DownloadDao
import com.erolit.app.data.local.dao.ReadingListDao
import com.erolit.app.data.local.dao.StoryDao
import com.erolit.app.data.local.entity.DownloadedPageEntity
import com.erolit.app.data.local.entity.ReadingListEntity
import com.erolit.app.data.local.entity.ReadingListStoryEntity
import com.erolit.app.data.local.entity.StoryEntity
import com.erolit.app.domain.model.ReadingList
import com.erolit.app.domain.model.Story
import com.erolit.app.domain.model.StoryPage
import com.erolit.app.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

import com.erolit.app.data.local.dao.ReadPageDao
import com.erolit.app.data.local.entity.ReadPageEntity
 
@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val storyDao: StoryDao,
    private val downloadDao: DownloadDao,
    private val readingListDao: ReadingListDao,
    private val readPageDao: ReadPageDao
) : LibraryRepository {

    // ─── Reading Lists ────────────────────────────────────────────────────────

    override fun getAllReadingLists(): Flow<List<ReadingList>> =
        readingListDao.getAllLists().map { lists ->
            lists.map { entity ->
                ReadingList(
                    id = entity.id,
                    name = entity.name,
                    description = entity.description,
                    createdAt = entity.createdAt
                )
            }
        }

    override suspend fun createReadingList(name: String, description: String): ReadingList {
        val list = ReadingList(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            createdAt = System.currentTimeMillis()
        )
        readingListDao.insertList(
            ReadingListEntity(list.id, list.name, list.description, list.createdAt)
        )
        return list
    }

    override suspend fun deleteReadingList(id: String) {
        readingListDao.deleteList(ReadingListEntity(id, "", "", 0L))
    }

    override suspend fun addStoryToList(listId: String, story: Story) {
        storyDao.upsertStory(story.toEntity())
        readingListDao.addStoryToList(ReadingListStoryEntity(listId, story.slug))
    }

    override suspend fun removeStoryFromList(listId: String, storySlug: String) {
        readingListDao.removeStoryFromList(listId, storySlug)
    }

    // ─── Saved Stories ────────────────────────────────────────────────────────

    override fun getSavedStories(): Flow<List<Story>> =
        storyDao.getSavedStories().map { it.map { e -> e.toDomain() } }

    override suspend fun getStory(slug: String): Story? =
        storyDao.getStoryBySlug(slug)?.toDomain()

    override suspend fun saveStory(story: Story) {
        // Single write: toEntity already encodes isSaved=true & savedAt.
        // A second updateSavedStatus call would produce a slightly later (different)
        // savedAt timestamp and trigger an unnecessary extra DB observer emit.
        storyDao.upsertStory(story.toEntity(isSaved = true, savedAt = System.currentTimeMillis()))
    }

    override suspend fun unsaveStory(slug: String) {
        storyDao.updateSavedStatus(slug, false, 0L)
    }

    override suspend fun isStorySaved(slug: String): Boolean =
        storyDao.isSaved(slug) ?: false

    // ─── Recently Read ────────────────────────────────────────────────────────

    override fun getRecentlyRead(): Flow<List<Story>> =
        storyDao.getRecentlyRead().map { it.map { e -> e.toDomain() } }

    override suspend fun markAsRead(story: Story) {
        // Ensure the story row exists (it may not if this is the first open).
        // We preserve isSaved/savedAt from any existing record and always set lastReadAt = 0
        // in toEntity — that's fine because we immediately call updateReadingProgress below,
        // which is the only function that sets lastReadAt correctly.
        val existing = storyDao.getStoryBySlug(story.slug)
        storyDao.upsertStory(
            story.toEntity(
                isSaved = existing?.isSaved ?: false,
                savedAt = existing?.savedAt ?: 0L
            )
        )
        // updateReadingProgress sets lastReadAt = System.currentTimeMillis(),
        // making the story visible in the "Recently Read" list.
        storyDao.updateReadingProgress(
            slug = story.slug,
            progress = story.readingProgress,
            page = if (story.readingProgress > 0f) story.pageCount.coerceAtLeast(1) else 1,
            readAt = System.currentTimeMillis()
        )
    }

    // ─── Reading Progress ─────────────────────────────────────────────────────

    override suspend fun saveReadingProgress(slug: String, scrollFraction: Float, lastPage: Int) {
        storyDao.updateReadingProgress(slug, scrollFraction, lastPage, System.currentTimeMillis())
    }

    override suspend fun getReadingProgress(slug: String): Pair<Float, Int>? {
        val entity = storyDao.getStoryBySlug(slug) ?: return null
        return Pair(entity.readingProgress, entity.lastPageRead)
    }

    // ─── Downloads ────────────────────────────────────────────────────────────

    override fun getDownloadedStories(): Flow<List<Story>> =
        storyDao.getDownloadedStories().map { it.map { e -> e.toDomain() } }

    override suspend fun saveDownloadedPages(slug: String, pages: List<StoryPage>) {
        downloadDao.deleteStoryPages(slug)
        downloadDao.insertPages(pages.map {
            DownloadedPageEntity(
                storySlug = it.storySlug,
                pageNumber = it.pageNumber,
                content = it.content,
                totalPages = it.totalPages
            )
        })
        // Critical: mark the story as downloaded so it shows up in the Downloads tab
        // If the story entity doesn't exist yet (race condition), insert a placeholder
        val existing = storyDao.getStoryBySlug(slug)
        if (existing == null) {
            storyDao.upsertStory(
                StoryEntity(
                    slug = slug,
                    title = slug.replace("-", " ").replaceFirstChar { it.uppercase() },
                    authorUsername = "",
                    authorDisplayName = "",
                    authorAvatarUrl = "",
                    categorySlug = "",
                    categoryName = "",
                    description = "",
                    rating = 0f,
                    ratingCount = 0,
                    commentCount = 0,
                    favoriteCount = 0,
                    wordCount = 0,
                    pageCount = pages.size,
                    tags = "",
                    datePublished = "",
                    isSaved = false,
                    isDownloaded = true,
                    savedAt = 0L,
                    readingProgress = 0f
                )
            )
        }
        storyDao.updateDownloadStatus(slug, true)
    }

    override suspend fun getDownloadedPages(slug: String): List<StoryPage> {
        return downloadDao.getPagesForStory(slug).map {
            StoryPage(it.storySlug, it.pageNumber, it.content, it.totalPages)
        }
    }

    override suspend fun deleteDownload(slug: String) {
        downloadDao.deleteStoryPages(slug)
        storyDao.updateDownloadStatus(slug, false)
    }

    override suspend fun isDownloaded(slug: String): Boolean =
        storyDao.isDownloaded(slug) ?: false
 
    // ─── Read Pages ───────────────────────────────────────────────────────────
 
    override fun getReadPages(slug: String): Flow<List<Int>> =
        readPageDao.getReadPages(slug)
 
    override suspend fun markPageAsRead(slug: String, pageNumber: Int) {
        readPageDao.markAsRead(ReadPageEntity(slug, pageNumber))
    }
 
    override suspend fun markPageAsUnread(slug: String, pageNumber: Int) {
        readPageDao.markAsUnread(ReadPageEntity(slug, pageNumber))
    }
}

// ─── Mapper Extensions ────────────────────────────────────────────────────────

private fun Story.toEntity(isSaved: Boolean = false, savedAt: Long = 0L): StoryEntity =
    StoryEntity(
        slug = slug,
        title = title,
        authorUsername = author.username,
        authorDisplayName = author.displayName,
        authorAvatarUrl = author.avatarUrl,
        categorySlug = category.slug,
        categoryName = category.name,
        description = description,
        rating = rating,
        ratingCount = ratingCount,
        commentCount = commentCount,
        favoriteCount = favoriteCount,
        wordCount = wordCount,
        pageCount = pageCount,
        tags = tags.joinToString(","),
        datePublished = datePublished,
        isSaved = isSaved,
        isDownloaded = isDownloaded,
        savedAt = savedAt,
        readingProgress = readingProgress
    )

private fun StoryEntity.toDomain(): Story {
    val author = com.erolit.app.domain.model.Author(
        username = authorUsername,
        displayName = authorDisplayName,
        avatarUrl = authorAvatarUrl
    )
    val category = com.erolit.app.domain.model.Category(
        slug = categorySlug,
        name = categoryName
    )
    return Story(
        slug = slug,
        title = title,
        author = author,
        category = category,
        description = description,
        rating = rating,
        ratingCount = ratingCount,
        commentCount = commentCount,
        favoriteCount = favoriteCount,
        wordCount = wordCount,
        pageCount = pageCount,
        tags = if (tags.isBlank()) emptyList() else tags.split(","),
        datePublished = datePublished,
        isSaved = isSaved,
        isDownloaded = isDownloaded,
        readingProgress = readingProgress
    )
}
