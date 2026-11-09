package dev.erolit.reader.data.repo

import dev.erolit.reader.core.db.SearchHistoryEntity
import dev.erolit.reader.core.net.ApiClient
import dev.erolit.reader.core.net.ApiRoutes
import dev.erolit.reader.core.net.AppJson
import dev.erolit.reader.core.net.Params
import dev.erolit.reader.data.categoryLabel
import dev.erolit.reader.data.dto.SearchMembersDto
import dev.erolit.reader.data.dto.SearchStoriesDto
import dev.erolit.reader.data.toDomain
import dev.erolit.reader.data.toSummary
import dev.erolit.reader.domain.SearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString

class SearchRepository(
    private val api: ApiClient,
    private val routes: ApiRoutes,
    private val categories: CategoryCache,
    private val db: dev.erolit.reader.core.db.AppDatabase,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun searchStories(
        query: String,
        page: Int,
        categoryIds: List<Int>,
        sort: String?,
    ): SearchResult {
        val dto = api.getJson(
            routes.searchStories(Params.search(query, page, categoryIds, sort)),
            retryTransient404 = true,
        ) { raw -> AppJson.decodeFromString<SearchStoriesDto>(raw) }
        val names = categories.names()
        val items = dto.data.map { submission ->
            submission.toSummary(categoryLabel(submission.category, submission.categoryInfo?.pageUrl, names))
        }
        recordHistory(query, "stories")
        return SearchResult(
            stories = items,
            total = dto.meta?.total ?: 0,
            page = page,
            pageSize = dto.meta?.pageSize ?: 50,
        )
    }

    suspend fun searchPoems(
        query: String,
        page: Int,
        categoryIds: List<Int>,
        sort: String?,
    ): SearchResult {
        val dto = api.getJson(
            routes.searchPoems(Params.search(query, page, categoryIds, sort)),
            retryTransient404 = true,
        ) { raw -> AppJson.decodeFromString<SearchStoriesDto>(raw) }
        val names = categories.names()
        val items = dto.data.map { submission ->
            submission.toSummary(categoryLabel(submission.category, submission.categoryInfo?.pageUrl, names))
        }
        recordHistory(query, "poems")
        return SearchResult(
            stories = items,
            total = dto.meta?.total ?: 0,
            page = page,
            pageSize = dto.meta?.pageSize ?: 50,
        )
    }

    suspend fun searchMembers(query: String, page: Int): SearchResult {
        val dto = api.getJson(
            routes.searchMembers(Params.search(query, page)),
            retryTransient404 = true,
        ) { raw -> AppJson.decodeFromString<SearchMembersDto>(raw) }
        recordHistory(query, "members")
        return SearchResult(
            members = dto.data.filter { it.userid > 0 }.map { it.toDomain() },
            total = dto.meta?.total ?: 0,
            page = page,
            pageSize = dto.meta?.pageSize ?: 50,
        )
    }

    fun history(): Flow<List<String>> =
        db.searchHistoryDao().observeRecent().map { rows -> rows.map { it.query }.distinct() }

    suspend fun clearHistory() {
        runCatching { db.searchHistoryDao().clear() }
    }

    private suspend fun recordHistory(query: String, type: String) {
        if (query.isBlank()) return
        runCatching {
            db.searchHistoryDao().upsert(
                SearchHistoryEntity(query = query.trim(), type = type, lastUsedAt = now()),
            )
        }
    }
}
