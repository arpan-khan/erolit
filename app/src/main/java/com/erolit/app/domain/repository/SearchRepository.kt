package com.erolit.app.domain.repository

import com.erolit.app.domain.model.Story
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    fun search(query: String): Flow<Result<List<Story>>>
    fun getSearchHistory(): Flow<List<String>>
    suspend fun addToHistory(query: String)
    suspend fun clearHistory()
}
