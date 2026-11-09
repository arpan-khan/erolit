package dev.erolit.reader.core.net

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The upstream API reads every argument from one URL-encoded JSON object named
 * `params`. Ordinary query parameters such as `page=2` are silently ignored.
 */
object Params {
    const val PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 50

    fun browse(
        page: Int,
        pageSize: Int = PAGE_SIZE,
        sortBy: String? = null,
        period: String? = null,
        categoryId: Int? = null,
        type: String? = null,
        language: Int? = null,
    ): String = buildJsonObject {
        put("page", page)
        put("pageSize", pageSize.coerceIn(1, MAX_PAGE_SIZE))
        sortBy?.let { put("sort_by", it) }
        period?.let { put("period", it) }
        categoryId?.let { put("category", it) }
        type?.let { put("type", it) }
        language?.let { put("language", it) }
    }.toString()

    /**
     * `/stories/new` is the source the website uses for "New Stories" and for the
     * "New" section of a category page. `/tagsportal/stories` returns the same
     * stories in a different (and incompatible) order.
     */
    fun newStories(
        page: Int,
        pageSize: Int = PAGE_SIZE,
        categoryId: Int? = null,
        period: String? = null,
        type: String? = "story",
        language: Int? = 1,
    ): String = buildJsonObject {
        put("page", page)
        put("pageSize", pageSize.coerceIn(1, MAX_PAGE_SIZE))
        categoryId?.let { put("category", it) }
        period?.let { put("period", it) }
        type?.let { put("type", it) }
        language?.let { put("language", it) }
    }.toString()

    /**
     * Top lists (Top Rated / Most Read / Most Favorited and the "Popular"
     * section of a category page) use a Laravel paginator. Omitting `period`
     * means "all time", which is the website default.
     */
    fun topList(
        page: Int,
        pageSize: Int = PAGE_SIZE,
        period: String? = null,
        categoryId: Int? = null,
        type: String? = null,
        language: Int? = 1,
    ): String = buildJsonObject {
        put("page", page)
        put("pageSize", pageSize.coerceIn(1, MAX_PAGE_SIZE))
        period?.let { put("period", it) }
        categoryId?.let { put("category", it) }
        type?.let { put("type", it) }
        language?.let { put("language", it) }
    }.toString()

    fun seriesFeed(language: Int? = 1): String = buildJsonObject {
        language?.let { put("language", it) }
    }.toString()

    fun storyPage(contentPage: Int): String = buildJsonObject {
        put("contentPage", contentPage.coerceAtLeast(1))
    }.toString()

    fun authorWorks(page: Int): String = buildJsonObject {
        put("page", page)
    }.toString()

    fun search(
        query: String,
        page: Int,
        categories: List<Int> = emptyList(),
        sort: String? = null,
    ): String = buildJsonObject {
        put("q", query)
        put("page", page)
        if (categories.isNotEmpty()) {
            put("categories", buildJsonArray { categories.forEach { add(JsonPrimitive(it)) } })
        }
        sort?.let { put("sort", it) }
    }.toString()

    fun listWorks(page: Int, sort: String = "dateadd"): String = buildJsonObject {
        put("page", page)
        put("sort", sort)
    }.toString()

    val empty: String = JsonObject(emptyMap()).toString()
}

val AppJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    isLenient = false
}
