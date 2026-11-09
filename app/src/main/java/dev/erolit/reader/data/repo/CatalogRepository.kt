package dev.erolit.reader.data.repo

import androidx.room.withTransaction
import dev.erolit.reader.core.db.AppDatabase
import dev.erolit.reader.core.db.CategoryEntity
import dev.erolit.reader.core.net.ApiClient
import dev.erolit.reader.core.net.ApiRoutes
import dev.erolit.reader.core.net.AppJson
import dev.erolit.reader.core.net.Params
import dev.erolit.reader.data.categoryLabel
import dev.erolit.reader.data.dto.BrowseDto
import dev.erolit.reader.data.dto.CategoryDto
import dev.erolit.reader.data.dto.PaginatedSubmissionsDto
import dev.erolit.reader.data.dto.SeriesFeedDto
import dev.erolit.reader.data.dto.SubmissionDto
import dev.erolit.reader.data.toDomain
import dev.erolit.reader.data.toSummary
import dev.erolit.reader.domain.Category
import dev.erolit.reader.domain.PagedStories
import dev.erolit.reader.domain.SeriesSummary
import dev.erolit.reader.domain.StorySummary
import kotlinx.serialization.decodeFromString

class CatalogRepository(
    private val api: ApiClient,
    private val routes: ApiRoutes,
    private val db: AppDatabase,
    private val cache: CategoryCache,
) {
    suspend fun loadCategories(force: Boolean = false): List<Category> {
        val cached = try {
            db.categoryDao().all().map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
        if (cached.isNotEmpty()) cache.update(cached)
        if (!force && cached.isNotEmpty()) return cache.all()

        val remote = try {
            api.getJson(routes.categories(), retryTransient404 = true) { raw ->
                AppJson.decodeFromString<List<CategoryDto>>(raw)
            }.map { it.toDomain() }
        } catch (e: Exception) {
            if (cached.isNotEmpty()) return cached
            throw e
        }

        val fetchedAt = System.currentTimeMillis()
        try {
            db.withTransaction {
                db.categoryDao().clear()
                db.categoryDao().insertAll(
                    remote.map {
                        CategoryEntity(
                            id = it.id,
                            name = it.name,
                            shortName = it.shortName,
                            description = it.description,
                            pageUrl = it.pageUrl,
                            type = it.type,
                            submissionCount = it.submissionCount,
                            fetchedAt = fetchedAt,
                        )
                    },
                )
            }
        } catch (e: Exception) {
            // A cache write failure must not break browsing.
        }
        cache.update(remote)
        return remote
    }

    suspend fun preload() {
        runCatching { loadCategories(force = false) }
    }

    /** Same source/order as the website "New Stories" page and a category's New section. */
    suspend fun newStories(page: Int): PagedStories =
        browseNew(Params.newStories(page = page), page)

    /**
     * Top Rated / Most Read / Most Favorited. The website does not use
     * `/tagsportal/stories?sort_by=...` for these pages; it uses the dedicated
     * `/stories/toprated|mostread|topfavorited` endpoints.
     */
    suspend fun topStories(page: Int, sortBy: String, period: String?): PagedStories {
        val params = Params.topList(page = page, period = period, type = "story")
        val url = when (sortBy) {
            "views" -> routes.mostRead(params)
            "favorite" -> routes.topFavorited(params)
            else -> routes.topRated(params)
        }
        return topList(url, page)
    }

    /** Same source/order as the New section of the website category page. */
    suspend fun categoryStories(page: Int, categoryId: Int): PagedStories =
        browseNew(Params.newStories(page = page, categoryId = categoryId), page)

    /** Same source/order as the Popular section of the website category page. */
    suspend fun categoryTopStories(page: Int, categoryId: Int, period: String?): PagedStories =
        topList(
            routes.topRated(
                Params.topList(
                    page = page,
                    period = period,
                    categoryId = categoryId,
                    type = "story",
                ),
            ),
            page,
        )

    suspend fun seriesFeed(kind: String): List<SeriesSummary> =
        api.getJson(routes.seriesFeed(kind, Params.seriesFeed()), retryTransient404 = true) { raw ->
            AppJson.decodeFromString<SeriesFeedDto>(raw)
        }.data.map { it.toSummary() }

    private suspend fun browseNew(params: String, page: Int): PagedStories {
        val dto = api.getJson(routes.newStories(params), retryTransient404 = true) { raw ->
            AppJson.decodeFromString<BrowseDto>(raw)
        }
        return toPaged(dto.submissions, page, dto.meta?.submissionsCount ?: 0, Params.PAGE_SIZE)
    }

    private suspend fun topList(url: String, page: Int): PagedStories {
        val dto = api.getJson(url, retryTransient404 = true) { raw ->
            AppJson.decodeFromString<PaginatedSubmissionsDto>(raw)
        }
        return toPaged(dto.data, page, dto.total, dto.perPage)
    }

    private fun toPaged(
        submissions: List<SubmissionDto>,
        page: Int,
        total: Int,
        pageSize: Int,
    ): PagedStories {
        val names = cache.names()
        val items = submissions.map { submission ->
            submission.toSummary(categoryLabel(submission.category, submission.categoryInfo?.pageUrl, names))
        }
        return PagedStories(
            items = items,
            page = page,
            total = total,
            pageSize = pageSize,
        )
    }

    private fun CategoryEntity.toDomain(): Category = Category(
        id = id,
        name = name,
        shortName = shortName,
        description = description,
        pageUrl = pageUrl,
        type = type,
        submissionCount = submissionCount,
    )
}
