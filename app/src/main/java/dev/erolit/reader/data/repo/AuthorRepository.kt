package dev.erolit.reader.data.repo

import dev.erolit.reader.core.net.ApiClient
import dev.erolit.reader.core.net.ApiError
import dev.erolit.reader.core.net.ApiRoutes
import dev.erolit.reader.core.net.AppJson
import dev.erolit.reader.core.net.Params
import dev.erolit.reader.data.categoryLabel
import dev.erolit.reader.data.dto.AuthorWorksDto
import dev.erolit.reader.data.dto.UserDetailDto
import dev.erolit.reader.data.dto.UserListDto
import dev.erolit.reader.data.toDomain
import dev.erolit.reader.data.toProfile
import dev.erolit.reader.data.toSummary
import dev.erolit.reader.domain.AuthorProfile
import dev.erolit.reader.domain.PagedStories
import dev.erolit.reader.domain.UserList
import kotlinx.serialization.decodeFromString

class AuthorRepository(
    private val api: ApiClient,
    private val routes: ApiRoutes,
    private val categories: CategoryCache,
) {
    suspend fun profile(id: Long): AuthorProfile {
        val dto = api.getJson(routes.author(id), retryTransient404 = true) { raw ->
            AppJson.decodeFromString<UserDetailDto>(raw)
        }
        return dto.user?.toProfile() ?: throw ApiError.NotFound
    }

    suspend fun works(id: Long, page: Int): PagedStories {
        val dto = api.getJson(
            routes.authorStories(id, Params.authorWorks(page)),
            retryTransient404 = true,
        ) { raw -> AppJson.decodeFromString<AuthorWorksDto>(raw) }
        val names = categories.names()
        return PagedStories(
            items = dto.data.map { submission ->
                submission.toSummary(categoryLabel(submission.category, submission.categoryInfo?.pageUrl, names))
            },
            page = dto.currentPage,
            total = dto.total,
            pageSize = dto.perPage.coerceAtMost(50),
        )
    }

    suspend fun publicLists(id: Long): List<UserList> =
        api.getJson(routes.authorLists(id), retryTransient404 = true) { raw ->
            AppJson.decodeFromString<List<UserListDto>>(raw)
        }.map { it.toDomain() }
}
