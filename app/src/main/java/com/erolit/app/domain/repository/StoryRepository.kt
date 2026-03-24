package com.erolit.app.domain.repository

import androidx.paging.PagingData
import com.erolit.app.domain.model.Story
import com.erolit.app.domain.model.StoryPage
import kotlinx.coroutines.flow.Flow

interface StoryRepository {
    fun getNewStories(page: Int = 1): Flow<Result<List<Story>>>
    fun getTopRatedStories(): Flow<Result<List<Story>>>
    fun getMostReadStories(): Flow<Result<List<Story>>>
    fun getCategoryStories(categorySlug: String): Flow<PagingData<Story>>
    suspend fun getStoryDetail(slug: String): Result<Story>
    suspend fun getStoryPage(slug: String, page: Int): Result<StoryPage>
    suspend fun getAllStoryPages(slug: String): Result<List<StoryPage>>
}
