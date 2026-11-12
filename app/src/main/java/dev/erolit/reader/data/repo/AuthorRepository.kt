package dev.erolit.reader.data.repo

import dev.erolit.reader.core.net.ApiClient
import dev.erolit.reader.core.net.ApiError
import dev.erolit.reader.core.net.ApiRoutes
import dev.erolit.reader.core.net.AppJson
import dev.erolit.reader.core.net.Params
import dev.erolit.reader.core.db.AppDatabase
import dev.erolit.reader.core.db.LocalAuthorEntity
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString

class AuthorRepository(
    private val api: ApiClient,
    private val routes: ApiRoutes,
    private val categories: CategoryCache,
    private val db: AppDatabase,
    private val now: () -> Long = { System.currentTimeMillis() },
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

    /** Authors saved on this device, newest first. */
    fun observeLocalAuthors(): Flow<List<AuthorProfile>> =
        db.localAuthorDao().observeAll().map { rows -> rows.map { it.toProfile() } }

    suspend fun isLocalAuthorSaved(authorId: Long): Boolean = try {
        db.localAuthorDao().find(authorId) != null
    } catch (e: Exception) {
        false
    }

    suspend fun saveLocalAuthor(profile: AuthorProfile) {
        try {
            db.localAuthorDao().upsert(profile.toEntity(now()))
        } catch (e: Exception) {
            // Saving an author is best effort; never crash the screen over it.
        }
    }

    suspend fun removeLocalAuthor(authorId: Long) {
        runCatching { db.localAuthorDao().delete(authorId) }
    }

    private fun AuthorProfile.toEntity(savedAt: Long): LocalAuthorEntity = LocalAuthorEntity(
        authorId = id,
        username = username,
        userpic = userpic,
        bio = bio,
        followers = followers,
        storiesCount = storiesCount,
        seriesCount = seriesCount,
        savedAt = savedAt,
    )

    private fun LocalAuthorEntity.toProfile(): AuthorProfile = AuthorProfile(
        id = authorId,
        username = username,
        userpic = userpic,
        bio = bio,
        followers = followers,
        storiesCount = storiesCount,
        seriesCount = seriesCount,
    )
}
