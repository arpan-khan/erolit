package com.erolit.app.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.erolit.app.data.remote.LiteroticaDataSource
import com.erolit.app.domain.model.Story

class CategoryStoriesPagingSource(
    private val dataSource: LiteroticaDataSource,
    private val categorySlug: String
) : PagingSource<Int, Story>() {

    override fun getRefreshKey(state: PagingState<Int, Story>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Story> {
        val page = params.key ?: 1
        return try {
            val stories = dataSource.getCategoryStories(categorySlug, page)
            LoadResult.Page(
                data = stories,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (stories.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
