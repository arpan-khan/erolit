package dev.erolit.reader.data.repo

import androidx.room.withTransaction
import dev.erolit.reader.core.db.AppDatabase
import dev.erolit.reader.core.db.HistoryEntity
import dev.erolit.reader.core.db.LocalSavedEntity
import dev.erolit.reader.core.db.LocalSeriesEntity
import dev.erolit.reader.core.db.LocalSeriesWorkEntity
import dev.erolit.reader.core.db.ProgressEntity
import dev.erolit.reader.core.db.RecentRow
import dev.erolit.reader.core.db.StoryEntity
import dev.erolit.reader.core.db.StoryPageEntity
import dev.erolit.reader.core.net.ApiClient
import dev.erolit.reader.core.net.ApiError
import dev.erolit.reader.core.net.ApiRoutes
import dev.erolit.reader.core.net.AppJson
import dev.erolit.reader.core.net.Params
import dev.erolit.reader.data.categoryLabel
import dev.erolit.reader.data.dto.StoryDetailDto
import dev.erolit.reader.data.toSummary
import dev.erolit.reader.domain.CachedStory
import dev.erolit.reader.domain.HistoryEntry
import dev.erolit.reader.domain.ProgressInfo
import dev.erolit.reader.domain.RecentReading
import dev.erolit.reader.domain.SeriesDetail
import dev.erolit.reader.domain.SeriesSummary
import dev.erolit.reader.domain.StoryDetail
import dev.erolit.reader.domain.StorySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString

class StoryRepository(
    private val api: ApiClient,
    private val routes: ApiRoutes,
    private val db: AppDatabase,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun fetchPage(idOrSlug: String, requestedPage: Int): StoryDetail {
        val page = requestedPage.coerceAtLeast(1)
        val dto = api.getJson(
            url = routes.story(idOrSlug, Params.storyPage(page)),
            retryTransient404 = true,
        ) { raw -> AppJson.decodeFromString<StoryDetailDto>(raw) }

        val submission = dto.submission ?: throw ApiError.Parse(IllegalStateException("missing submission"))
        val pages = dto.meta?.pagesCount?.coerceAtLeast(1) ?: 1
        val text = dto.pageText.orEmpty()
        val summary = submission.toSummary(
            categoryLabel(submission.category, submission.categoryInfo?.pageUrl, emptyMap()),
        )
        val fetchedAt = now()

        try {
            db.withTransaction {
                db.storyDao().upsert(summary.toEntity(pagesCount = pages, updatedAt = fetchedAt))
                db.storyDao().upsertPage(
                    StoryPageEntity(storyId = summary.id, page = page, text = text, fetchedAt = fetchedAt),
                )
            }
        } catch (e: Exception) {
            // Reading must still work if the cache write fails.
        }

        return StoryDetail(
            summary = summary,
            pagesCount = pages,
            page = page,
            text = text,
            fetchedAt = fetchedAt,
        )
    }

    suspend fun cachedStory(storyId: Long): CachedStory? {
        val story = try {
            db.storyDao().find(storyId)
        } catch (e: Exception) {
            null
        } ?: return null
        return CachedStory(
            summary = story.toSummary(),
            pagesCount = story.pagesCount?.coerceAtLeast(1) ?: 1,
            page = 1,
            text = null,
            fetchedAt = story.updatedAt,
        )
    }

    suspend fun cachedPage(storyId: Long, page: Int): StoryPageEntity? = try {
        db.storyDao().findPage(storyId, page)
    } catch (e: Exception) {
        null
    }

    suspend fun progress(storyId: Long): ProgressInfo? = try {
        db.progressDao().find(storyId)?.toDomain()
    } catch (e: Exception) {
        null
    }

    suspend fun progressFor(storyIds: List<Long>): Map<Long, ProgressInfo> = try {
        db.progressDao().findFor(storyIds).associate { it.storyId to it.toDomain() }
    } catch (e: Exception) {
        emptyMap()
    }

    /** Every stored progress row, keyed by story id. Backed by a Room Flow so lists refresh live. */
    fun observeProgress(): Flow<Map<Long, ProgressInfo>> =
        db.progressDao().observeAll().map { rows -> rows.associate { it.storyId to it.toDomain() } }

    suspend fun saveProgress(storyId: Long, page: Int, scrollFraction: Float, pagesCount: Int) {
        val safePages = pagesCount.coerceAtLeast(1)
        val safePage = page.coerceIn(1, safePages)
        val fraction = scrollFraction.coerceIn(0f, 1f)
        // Always store where the reader currently is, so scrolling back up
        // lowers the percentage again.
        val percent = (((safePage - 1) + fraction) / safePages.toFloat()).coerceIn(0f, 1f)
        try {
            db.progressDao().upsert(
                ProgressEntity(
                    storyId = storyId,
                    page = safePage,
                    scrollFraction = fraction,
                    percent = percent,
                    updatedAt = now(),
                ),
            )
        } catch (e: Exception) {
            // Progress is best effort; never crash the reader over a DB write.
        }
    }

    /** Marks a story as finished without touching its saved position. */
    suspend fun markStoryRead(storyId: Long) {
        try {
            db.progressDao().upsert(
                ProgressEntity(
                    storyId = storyId,
                    page = 1,
                    scrollFraction = 1f,
                    percent = 1f,
                    updatedAt = now(),
                ),
            )
        } catch (e: Exception) {
            // Marking read is best effort; never crash the UI over a DB write.
        }
    }

    /** Clears a story's progress so it appears unread again. */
    suspend fun markStoryUnread(storyId: Long) {
        runCatching { db.progressDao().delete(storyId) }
    }

    /** Marks every chapter of a series as finished. */
    suspend fun markSeriesRead(seriesId: Long, chapterIds: List<Long> = emptyList()) {
        val ids = chapterIds.ifEmpty {
            try {
                db.localSeriesDao().works(seriesId).map { it.storyId }
            } catch (e: Exception) {
                emptyList()
            }
        }
        if (ids.isEmpty()) return
        try {
            val timestamp = now()
            db.progressDao().upsertAll(
                ids.map { storyId ->
                    ProgressEntity(
                        storyId = storyId,
                        page = 1,
                        scrollFraction = 1f,
                        percent = 1f,
                        updatedAt = timestamp,
                    )
                },
            )
        } catch (e: Exception) {
            // Marking a series read is best effort.
        }
    }

    /** Clears progress for every chapter of a series. */
    suspend fun markSeriesUnread(seriesId: Long, chapterIds: List<Long> = emptyList()) {
        val ids = chapterIds.ifEmpty {
            try {
                db.localSeriesDao().works(seriesId).map { it.storyId }
            } catch (e: Exception) {
                emptyList()
            }
        }
        if (ids.isEmpty()) return
        runCatching { db.progressDao().deleteFor(ids) }
    }

    fun observeRecent(): Flow<List<RecentReading>> =
        db.progressDao().observeRecent().map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    /** The on-device saved list. Not tied to an account and never synced. */
    fun observeLocalSaved(): Flow<List<StorySummary>> =
        db.localSavedDao().observeAll().map { rows -> rows.map { it.toSummary() } }

    suspend fun isLocalSaved(storyId: Long): Boolean = try {
        db.localSavedDao().find(storyId) != null
    } catch (e: Exception) {
        false
    }

    suspend fun saveLocal(story: StorySummary) {
        try {
            db.localSavedDao().upsert(story.toLocalEntity(now()))
        } catch (e: Exception) {
            // Saving locally is best effort; never crash the reader over it.
        }
    }

    suspend fun removeLocal(storyId: Long) {
        runCatching { db.localSavedDao().delete(storyId) }
    }

    /**
     * Records that a story was opened. One row per story is kept, so opening
     * a story again moves it to the top of History instead of duplicating it.
     */
    suspend fun recordHistory(story: StorySummary) {
        try {
            db.historyDao().upsert(story.toHistoryEntity(now()))
        } catch (e: Exception) {
            // History is best effort; never crash the reader over a DB write.
        }
    }

    /** Every story the reader opened, most recent first. */
    fun observeHistory(): Flow<List<HistoryEntry>> =
        db.historyDao().observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun clearHistory() {
        runCatching { db.historyDao().clear() }
    }

    suspend fun clearCachedPages() {
        runCatching { db.storyDao().clearAllPages() }
    }

    /** Series saved for local browsing, newest first. */
    fun observeLocalSeries(): Flow<List<SeriesSummary>> =
        combine(
            db.localSeriesDao().observeAll(),
            db.localSeriesDao().observeAllWorks(),
        ) { rows, works ->
            val idsBySeries = works.groupBy { it.seriesId }
                .mapValues { (_, items) -> items.sortedBy { it.position }.map { it.storyId } }
            rows.map { row -> row.toSummary(idsBySeries[row.seriesId].orEmpty()) }
        }

    suspend fun localSeries(seriesId: Long): SeriesDetail? = try {
        val row = db.localSeriesDao().find(seriesId) ?: return null
        val works = db.localSeriesDao().works(seriesId).map { it.toSummary(row.title) }
        SeriesDetail(
            id = row.seriesId,
            url = null,
            title = row.title,
            authorName = row.authorName,
            authorId = row.authorId,
            description = row.description,
            introduction = null,
            workCount = row.workCount,
            state = row.state,
            works = works,
        )
    } catch (e: Exception) {
        null
    }

    suspend fun isLocalSeriesSaved(seriesId: Long): Boolean = try {
        db.localSeriesDao().find(seriesId) != null
    } catch (e: Exception) {
        false
    }

    suspend fun saveLocalSeries(detail: SeriesDetail) {
        try {
            db.withTransaction {
                db.localSeriesDao().upsertSeries(
                    LocalSeriesEntity(
                        seriesId = detail.id,
                        title = detail.title,
                        authorId = detail.authorId,
                        authorName = detail.authorName,
                        description = detail.description,
                        state = detail.state,
                        workCount = detail.works.size.takeIf { it > 0 } ?: detail.workCount,
                        savedAt = now(),
                    ),
                )
                db.localSeriesDao().clearWorks(detail.id)
                db.localSeriesDao().insertWorks(
                    detail.works.mapIndexed { index, work ->
                        LocalSeriesWorkEntity(
                            seriesId = detail.id,
                            storyId = work.id,
                            position = index,
                            slug = work.slug,
                            title = work.title,
                            authorId = work.authorId,
                            authorName = work.authorName,
                            authorPic = work.authorPic,
                            categoryId = work.categoryId,
                            categoryName = work.categoryName,
                            description = work.description,
                            words = work.words,
                            readingMinutes = work.readingMinutes,
                            rating = work.rating,
                            ratingCount = work.ratingCount,
                            publishedAt = work.publishedAt,
                            type = work.type,
                            tags = work.tags.encodeTags(),
                        )
                    },
                )
            }
        } catch (e: Exception) {
            // Saving a series is best effort; never crash the screen over it.
        }
    }

    suspend fun removeLocalSeries(seriesId: Long) {
        runCatching { db.localSeriesDao().delete(seriesId) }
    }

    private fun StoryEntity.toSummary(): StorySummary = StorySummary(
        id = id,
        slug = slug,
        title = title,
        authorId = authorId,
        authorName = authorName,
        authorPic = authorPic,
        categoryId = categoryId,
        categoryName = categoryName,
        description = description,
        words = words,
        readingMinutes = readingMinutes,
        rating = rating,
        ratingCount = ratingCount,
        publishedAt = publishedAt,
        type = type,
        isHot = false,
        isNew = false,
        writersPick = false,
        seriesId = seriesId,
        seriesTitle = seriesTitle,
        tags = tags.decodeTags(),
    )

    private fun StorySummary.toEntity(pagesCount: Int, updatedAt: Long): StoryEntity = StoryEntity(
        id = id,
        slug = slug,
        title = title,
        authorId = authorId,
        authorName = authorName,
        authorPic = authorPic,
        categoryId = categoryId,
        categoryName = categoryName,
        description = description,
        words = words,
        readingMinutes = readingMinutes,
        rating = rating,
        ratingCount = ratingCount,
        publishedAt = publishedAt,
        type = type,
        pagesCount = pagesCount,
        seriesId = seriesId,
        seriesTitle = seriesTitle,
        tags = tags.encodeTags(),
        updatedAt = updatedAt,
    )

    private fun StorySummary.toLocalEntity(savedAt: Long): LocalSavedEntity = LocalSavedEntity(
        storyId = id,
        slug = slug,
        title = title,
        authorId = authorId,
        authorName = authorName,
        categoryId = categoryId,
        categoryName = categoryName,
        description = description,
        words = words,
        readingMinutes = readingMinutes,
        rating = rating,
        ratingCount = ratingCount,
        publishedAt = publishedAt,
        type = type,
        seriesId = seriesId,
        seriesTitle = seriesTitle,
        tags = tags.encodeTags(),
        savedAt = savedAt,
    )

    private fun LocalSavedEntity.toSummary(): StorySummary = StorySummary(
        id = storyId,
        slug = slug,
        title = title,
        authorId = authorId,
        authorName = authorName,
        authorPic = null,
        categoryId = categoryId,
        categoryName = categoryName,
        description = description,
        words = words,
        readingMinutes = readingMinutes,
        rating = rating,
        ratingCount = ratingCount,
        publishedAt = publishedAt,
        type = type,
        isHot = false,
        isNew = false,
        writersPick = false,
        seriesId = seriesId,
        seriesTitle = seriesTitle,
        tags = tags.decodeTags(),
    )

    private fun StorySummary.toHistoryEntity(openedAt: Long): HistoryEntity = HistoryEntity(
        storyId = id,
        slug = slug,
        title = title,
        authorId = authorId,
        authorName = authorName,
        authorPic = authorPic,
        categoryId = categoryId,
        categoryName = categoryName,
        description = description,
        words = words,
        readingMinutes = readingMinutes,
        rating = rating,
        ratingCount = ratingCount,
        publishedAt = publishedAt,
        type = type,
        seriesId = seriesId,
        seriesTitle = seriesTitle,
        tags = tags.encodeTags(),
        openedAt = openedAt,
    )

    private fun HistoryEntity.toDomain(): HistoryEntry = HistoryEntry(
        story = StorySummary(
            id = storyId,
            slug = slug,
            title = title,
            authorId = authorId,
            authorName = authorName,
            authorPic = authorPic,
            categoryId = categoryId,
            categoryName = categoryName,
            description = description,
            words = words,
            readingMinutes = readingMinutes,
            rating = rating,
            ratingCount = ratingCount,
            publishedAt = publishedAt,
            type = type,
            isHot = false,
            isNew = false,
            writersPick = false,
            seriesId = seriesId,
            seriesTitle = seriesTitle,
            tags = tags.decodeTags(),
        ),
        openedAt = openedAt,
    )

    private fun LocalSeriesEntity.toSummary(workIds: List<Long>): SeriesSummary = SeriesSummary(
        id = seriesId,
        url = null,
        title = title,
        authorName = authorName,
        authorId = authorId,
        description = description,
        state = state,
        workCount = workCount,
        workIds = workIds,
    )

    private fun LocalSeriesWorkEntity.toSummary(seriesTitle: String): StorySummary = StorySummary(
        id = storyId,
        slug = slug,
        title = title,
        authorId = authorId,
        authorName = authorName,
        authorPic = authorPic,
        categoryId = categoryId,
        categoryName = categoryName,
        description = description,
        words = words,
        readingMinutes = readingMinutes,
        rating = rating,
        ratingCount = ratingCount,
        publishedAt = publishedAt,
        type = type,
        isHot = false,
        isNew = false,
        writersPick = false,
        seriesId = seriesId,
        seriesTitle = seriesTitle,
        tags = tags.decodeTags(),
    )

    private fun List<String>.encodeTags(): String? =
        map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            .takeIf { it.isNotEmpty() }
            ?.joinToString("\n")

    private fun String?.decodeTags(): List<String> =
        this?.split('\n')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()

    private fun ProgressEntity.toDomain(): ProgressInfo = ProgressInfo(
        storyId = storyId,
        page = page,
        scrollFraction = scrollFraction,
        percent = percent,
        updatedAt = updatedAt,
    )

    private fun RecentRow.toDomainOrNull(): RecentReading? {
        val title = title ?: return null
        return RecentReading(
            storyId = storyId,
            page = page,
            scrollFraction = scrollFraction,
            percent = percent,
            updatedAt = updatedAt,
            title = title,
            authorName = authorName,
            slug = slug,
            type = type,
        )
    }
}
