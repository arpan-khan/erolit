package com.erolit.app.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.erolit.app.data.local.dao.StoryDao
import com.erolit.app.data.paging.CategoryStoriesPagingSource
import com.erolit.app.data.remote.LiteroticaDataSource
import com.erolit.app.domain.model.Story
import com.erolit.app.domain.model.StoryPage
import com.erolit.app.domain.repository.StoryRepository
import com.erolit.app.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryRepositoryImpl @Inject constructor(
    private val dataSource: LiteroticaDataSource,
    private val storyDao: StoryDao
) : StoryRepository {

    override fun getNewStories(page: Int): Flow<Result<List<Story>>> = flow {
        runCatching { dataSource.getNewStories(page) }
            .onSuccess { emit(Result.success(it)) }
            .onFailure { emit(Result.failure(it)) }
    }

    override fun getTopRatedStories(): Flow<Result<List<Story>>> = flow {
        runCatching { dataSource.getTopRatedStories() }
            .onSuccess { emit(Result.success(it)) }
            .onFailure { emit(Result.failure(it)) }
    }

    override fun getMostReadStories(): Flow<Result<List<Story>>> = flow {
        runCatching { dataSource.getMostReadStories() }
            .onSuccess { emit(Result.success(it)) }
            .onFailure { emit(Result.failure(it)) }
    }

    override fun getCategoryStories(categorySlug: String): Flow<PagingData<Story>> {
        return Pager(
            config = PagingConfig(pageSize = Constants.PAGE_SIZE, enablePlaceholders = false),
            pagingSourceFactory = { CategoryStoriesPagingSource(dataSource, categorySlug) }
        ).flow
    }

    override suspend fun getStoryDetail(slug: String): Result<Story> {
        return runCatching { dataSource.getStoryDetail(slug) }
    }

    override suspend fun getStoryPage(slug: String, page: Int): Result<StoryPage> {
        return runCatching { dataSource.getStoryPage(slug, page) }
    }

    override suspend fun getAllStoryPages(slug: String): Result<List<StoryPage>> = runCatching {
        val detail = dataSource.getStoryDetail(slug)
        val pages = mutableListOf<StoryPage>()
        for (i in 1..detail.pageCount) {
            pages.add(dataSource.getStoryPage(slug, i))
        }
        pages
    }
}
