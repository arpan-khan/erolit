package dev.erolit.reader.data.repo

import dev.erolit.reader.core.net.ApiClient
import dev.erolit.reader.core.net.ApiError
import dev.erolit.reader.core.net.ApiRoutes
import dev.erolit.reader.core.net.AppJson
import dev.erolit.reader.core.net.Params
import dev.erolit.reader.data.categoryLabel
import dev.erolit.reader.data.dto.SeriesDetailDto
import dev.erolit.reader.data.dto.SubmissionDto
import dev.erolit.reader.data.toSummary
import dev.erolit.reader.domain.SeriesDetail
import dev.erolit.reader.domain.StorySummary
import kotlinx.serialization.decodeFromString

class SeriesRepository(
    private val api: ApiClient,
    private val routes: ApiRoutes,
    private val categories: CategoryCache,
) {
    suspend fun load(idOrSlug: String): SeriesDetail {
        val meta = api.getJson(routes.series(idOrSlug), retryTransient404 = true) { raw ->
            AppJson.decodeFromString<SeriesDetailDto>(raw)
        }.data ?: throw ApiError.NotFound

        val works = api.getJson(routes.seriesWorks(idOrSlug), retryTransient404 = true) { raw ->
            AppJson.decodeFromString<List<SubmissionDto>>(raw)
        }.map { it.toSummary(worksCategoryName(it)) }

        return SeriesDetail(
            id = meta.id,
            url = meta.url,
            title = meta.title,
            authorName = meta.user?.username ?: meta.authorName,
            authorId = meta.user?.userid ?: meta.userId,
            description = meta.description,
            introduction = meta.introduction,
            workCount = meta.workCount ?: works.size,
            state = meta.state,
            works = works,
        )
    }

    private fun worksCategoryName(dto: SubmissionDto): String? =
        categoryLabel(dto.category, dto.categoryInfo?.pageUrl, categories.names())
}
