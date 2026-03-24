package com.erolit.app.data.repository

import com.erolit.app.data.local.dao.SearchHistoryDao
import com.erolit.app.data.local.entity.SearchHistoryEntity
import com.erolit.app.data.remote.LiteroticaDataSource
import com.erolit.app.domain.model.Story
import com.erolit.app.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val dataSource: LiteroticaDataSource,
    private val searchHistoryDao: SearchHistoryDao
) : SearchRepository {

    override fun search(query: String): Flow<Result<List<Story>>> = flow {
        runCatching { dataSource.searchStories(query) }
            .onSuccess { emit(Result.success(it)) }
            .onFailure { emit(Result.failure(it)) }
    }

    override fun getSearchHistory(): Flow<List<String>> =
        searchHistoryDao.getHistory().map { list -> list.map { it.query } }

    override suspend fun addToHistory(query: String) {
        searchHistoryDao.insert(SearchHistoryEntity(query = query))
    }

    override suspend fun clearHistory() = searchHistoryDao.clearAll()
}
