package com.erolit.app.data.remote

import com.erolit.app.domain.model.Author
import com.erolit.app.domain.model.Series
import com.erolit.app.domain.model.Story
import com.erolit.app.domain.model.StoryPage
import com.erolit.app.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteroticaDataSource @Inject constructor(
    private val client: OkHttpClient,
    private val parser: LiteroticaParser
) {

    private suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        // Headers (User-Agent, Accept, Accept-Language, Referer) are added by the
        // OkHttpClient interceptor in NetworkModule — no need to duplicate them here.
        val request = Request.Builder()
            .url(url)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}: ${response.message}")
            response.body?.string() ?: throw Exception("Empty response body")
        }
    }

    // ─── Stories ─────────────────────────────────────────────────────────────

    suspend fun getStoryDetail(slug: String): Story {
        val url = "${Constants.BASE_URL}${Constants.PATH_STORY}$slug"
        val html = fetchHtml(url)
        return parser.parseStoryDetail(html, slug)
    }

    suspend fun getStoryPage(slug: String, page: Int): StoryPage {
        val url = if (page == 1) {
            "${Constants.BASE_URL}${Constants.PATH_STORY}$slug"
        } else {
            "${Constants.BASE_URL}${Constants.PATH_STORY}$slug?page=$page"
        }
        val html = fetchHtml(url)
        return parser.parseStoryPageContent(html, slug, page)
    }

    // ─── Listings ──────────────────────────────────────────────────────────

    suspend fun getNewStories(page: Int = 1): List<Story> {
        val url = "${Constants.BASE_URL}${Constants.PATH_NEW}?page=$page"
        val html = fetchHtml(url)
        return parser.parseStoryListing(html)
    }

    suspend fun getTopRatedStories(): List<Story> {
        val url = "${Constants.BASE_URL}${Constants.PATH_TOP_RATED}"
        val html = fetchHtml(url)
        return parser.parseStoryListing(html)
    }

    suspend fun getMostReadStories(): List<Story> {
        val url = "${Constants.BASE_URL}${Constants.PATH_MOST_READ}"
        val html = fetchHtml(url)
        return parser.parseStoryListing(html)
    }

    suspend fun getCategoryStories(categorySlug: String, page: Int = 1): List<Story> {
        val url = if (page == 1) {
            "${Constants.BASE_URL}${Constants.PATH_CATEGORY}$categorySlug"
        } else {
            "${Constants.BASE_URL}${Constants.PATH_CATEGORY}$categorySlug/$page-page"
        }
        val html = fetchHtml(url)
        return parser.parseStoryListing(html)
    }

    suspend fun getCategoryTotalPages(categorySlug: String): Int {
        val url = "${Constants.BASE_URL}${Constants.PATH_CATEGORY}$categorySlug"
        val html = fetchHtml(url)
        return parser.parseTotalListingPages(html)
    }

    // ─── Author ──────────────────────────────────────────────────────────────

    suspend fun getAuthor(username: String): Author {
        val url = "${Constants.BASE_URL}${Constants.PATH_AUTHOR}$username"
        val html = fetchHtml(url)
        return parser.parseAuthorProfile(html, username)
    }

    suspend fun getAuthorStories(username: String): List<Story> {
        val url = "${Constants.BASE_URL}${Constants.PATH_AUTHOR}$username/works/stories"
        val html = fetchHtml(url)
        return parser.parseAuthorStories(html)
    }

    // ─── Series ──────────────────────────────────────────────────────────────

    suspend fun getSeries(seriesId: String): Series {
        val url = "${Constants.BASE_URL}${Constants.PATH_SERIES}se/$seriesId"
        val html = fetchHtml(url)
        return parser.parseSeries(html, seriesId)
    }

    // ─── Tags ────────────────────────────────────────────────────────────────

    suspend fun getTagStories(tagName: String, page: Int = 1): List<Story> {
        val url = "${Constants.TAGS_URL}/$tagName/?page=$page"
        val html = fetchHtml(url)
        return parser.parseTagStories(html)
    }

    // ─── Search ──────────────────────────────────────────────────────────────

    suspend fun searchStories(query: String): List<Story> {
        // Use tags as an alternative to search (which is an SPA)
        val encodedQuery = query.trim().replace(" ", "-").lowercase()
        val url = "${Constants.TAGS_URL}/$encodedQuery"
        return try {
            val html = fetchHtml(url)
            parser.parseStoryListing(html)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
