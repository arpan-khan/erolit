package com.erolit.app.domain.repository

import com.erolit.app.domain.model.ReadingList
import com.erolit.app.domain.model.Story
import com.erolit.app.domain.model.StoryPage
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    // Reading lists
    fun getAllReadingLists(): Flow<List<ReadingList>>
    suspend fun createReadingList(name: String, description: String = ""): ReadingList
    suspend fun deleteReadingList(id: String)
    suspend fun addStoryToList(listId: String, story: Story)
    suspend fun removeStoryFromList(listId: String, storySlug: String)

    // Saved stories
    fun getSavedStories(): Flow<List<Story>>
    suspend fun getStory(slug: String): Story?
    suspend fun saveStory(story: Story)
    suspend fun unsaveStory(slug: String)
    suspend fun isStorySaved(slug: String): Boolean

    // Recently read
    fun getRecentlyRead(): Flow<List<Story>>
    suspend fun markAsRead(story: Story)

    // Reading progress
    suspend fun saveReadingProgress(slug: String, scrollFraction: Float, lastPage: Int)
    suspend fun getReadingProgress(slug: String): Pair<Float, Int>?

    // Downloads
    fun getDownloadedStories(): Flow<List<Story>>
    suspend fun saveDownloadedPages(slug: String, pages: List<StoryPage>)
    suspend fun getDownloadedPages(slug: String): List<StoryPage>
    suspend fun deleteDownload(slug: String)
    suspend fun isDownloaded(slug: String): Boolean
    // Story read status per page
    fun getReadPages(slug: String): Flow<List<Int>>
    suspend fun markPageAsRead(slug: String, pageNumber: Int)
    suspend fun markPageAsUnread(slug: String, pageNumber: Int)
}
