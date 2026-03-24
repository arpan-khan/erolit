package com.erolit.app.data.remote

import com.erolit.app.domain.model.Author
import com.erolit.app.domain.model.Category
import com.erolit.app.domain.model.Series
import com.erolit.app.domain.model.Story
import com.erolit.app.domain.model.StoryPage
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteroticaParser @Inject constructor() {

    // ─── Story Detail Page ───────────────────────────────────────────────────

    /**
     * Parses a full story page from /s/{slug} or /s/{slug}?page=N
     * Returns a Story with metadata populated.
     */
    fun parseStoryDetail(html: String, slug: String): Story {
        val doc = Jsoup.parse(html)
        val title = doc.selectFirst("h1.j_wd")?.text()
            ?: doc.selectFirst("h1")?.text()
            ?: "Unknown Title"

        // Author
        val authorEl = doc.selectFirst("a.y_eU") ?: doc.selectFirst(".b-story-user-y a")
        val authorUsername = authorEl?.attr("href")
            ?.removePrefix("https://www.literotica.com/authors/")
            ?.trimEnd('/') ?: ""
        val authorName = authorEl?.text() ?: authorUsername

        // Avatar
        val avatarUrl = doc.selectFirst("img.j_au")?.attr("abs:src")
            ?: doc.selectFirst(".b-story-user-y img")?.attr("abs:src") ?: ""

        // Category
        val categoryEl = doc.selectFirst("a.y_eS") ?: doc.selectFirst(".b-story-tag-cats a")
        val categoryName = categoryEl?.text() ?: ""
        val categorySlug = categoryEl?.attr("href")
            ?.substringAfter("/c/")?.trimEnd('/') ?: ""

        // Tags
        val tags = doc.select("a.y_eT, .b-story-tag a").map { it.text() }.filter { it.isNotBlank() }

        // Rating
        var rating = doc.selectFirst("span.b-r-value, .j_ra, meta[itemprop=ratingValue]")?.let { 
            if (it.tagName() == "meta") it.attr("content") else it.text().trim() 
        }?.toFloatOrNull() ?: 0f

        if (rating == 0f) {
            // Robust regex fallback: find first float between 1.0 and 5.0
            val allText = doc.body().text()
            rating = Regex("\\b([1-4]\\.[0-9]{2})\\b").findAll(allText)
                        .firstOrNull()?.value?.toFloatOrNull() ?: 0f
        }

        // Word count
        val metaItems = doc.select("span.b-story-meta span, .j_wc")
        val wordCountStr = metaItems.firstOrNull()?.text()?.replace(",", "")?.replace("k", "000") ?: "0"
        val wordCount = wordCountStr.filter { it.isDigit() }.toIntOrNull() ?: 0

        // Comments & favorites
        val commentCount = doc.selectFirst("span.g_co")?.text()?.trim()?.toIntOrNull() ?: 0
        val favoriteCount = doc.selectFirst("span.g_fa")?.text()?.trim()?.toIntOrNull() ?: 0


        // Date - more robust
        val datePublished = doc.selectFirst("span.b-story-meta span:last-child, .j_da, .b-story-meta--date")?.text()?.trim() ?: ""

        // Author meta (works, followers)
        val authorLinks = doc.select("a[href*='/authors/$authorUsername']")
        val storyCount = authorLinks.firstOrNull { it.text().contains("Stories") }
            ?.text()?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        val followers = authorLinks.firstOrNull { it.text().contains("Followers") }
            ?.text()?.replace(",", "")?.filter { it.isDigit() }?.toIntOrNull() ?: 0

        // Total pages extraction via href as described in the reference
        val pageNumbers = doc.select("a[href*='?page='], a[href*='page=']")
            .mapNotNull { 
                var param = it.attr("href").substringAfter("page=", "")
                if (param.isBlank()) param = it.attr("href").substringAfter("?page=", "")
                param.substringBefore("&").toIntOrNull() 
            }
        val maxPage = pageNumbers.maxOrNull() ?: 1
        val totalPages = maxOf(1, maxPage)

        return Story(
            slug = slug,
            title = title,
            description = doc.selectFirst(".b-story-description, .j_jt, div.aa_ht")?.text()?.trim() ?: "",
            author = Author(
                username = authorUsername,
                displayName = authorName,
                avatarUrl = avatarUrl,
                storyCount = storyCount,
                followerCount = followers
            ),
            category = Category(slug = categorySlug, name = categoryName),
            rating = rating,
            commentCount = commentCount,
            favoriteCount = favoriteCount,
            wordCount = wordCount,
            pageCount = totalPages,
            tags = tags,
            datePublished = datePublished
        )
    }

    /**
     * Parses the story body text from a story page (stripping HTML, keeping paragraphs).
     */
    fun parseStoryPageContent(html: String, slug: String, pageNumber: Int): StoryPage {
        val doc = Jsoup.parse(html)

        // ── 1. Find the story body container ─────────────────────────────────
        // Multiple selectors to handle the classic & beta lite sites
        val bodyEl = doc.selectFirst("div[data-field='body']")
            ?: doc.selectFirst("div.aa_wr")
            ?: doc.selectFirst("div.b-story-body-x")
            ?: doc.selectFirst("div.b-story-body")
            ?: doc.selectFirst("div.aa_ht")

        val paragraphs: List<String> = if (bodyEl != null) {
            // If the beta site uses <br> tags instead of <p> tags, convert them
            if (bodyEl.select("p").isEmpty() && bodyEl.html().contains("<br>")) {
                bodyEl.html(bodyEl.html().replace("<br>", "</p><p>"))
            }
            bodyEl.select("p").map { it.text().trim() }.filter { it.isNotBlank() }
        } else {
            // Fallback: grab all <p> from the body but skip known nav/UI containers
            doc.body().select("p").filterNot { el ->
                el.parents().any { p ->
                    p.className().let { c ->
                        c.contains("b-pager") || c.contains("b-footer") ||
                        c.contains("qni-") || c.contains("comment") || c.contains("header")
                    }
                }
            }.map { it.text().trim() }.filter { it.isNotBlank() }
        }

        // ── 2. Find the longest contiguous run of content paragraphs ─────────
        data class Run(val start: Int, val end: Int)
        var bestRun = Run(0, 0)
        var currentStart = -1
        var currentLength = 0

        for (i in paragraphs.indices) {
            val isContent = paragraphs[i].length >= 30
            if (isContent || currentLength >= 3) {
                if (currentStart == -1) currentStart = i
                currentLength++
                if (currentLength > bestRun.end - bestRun.start) {
                    bestRun = Run(currentStart, i + 1)
                }
            } else {
                currentStart = -1
                currentLength = 0
            }
        }

        val content = if (bestRun.end > bestRun.start) {
            paragraphs.subList(bestRun.start, bestRun.end).joinToString("\n\n")
        } else {
            paragraphs.filter { it.length >= 30 }.joinToString("\n\n")
        }.ifBlank { 
            // Last resort — just dump all cleaned text from the body element if available
            (bodyEl ?: doc.body()).text().trim() 
        }

        // ── 3. Extract total page count from pagination hrefs ─────────────────
        val pageNumbers = doc.select("a[href*='?page='], a[href*='&page=']")
            .mapNotNull { a ->
                val href = a.attr("href")
                val afterParam = href.substringAfter("?page=", "")
                    .ifEmpty { href.substringAfter("&page=", "") }
                afterParam.substringBefore("&").substringBefore("#").toIntOrNull()
            }
        val maxPage = pageNumbers.maxOrNull() ?: 1
        val totalPages = maxOf(pageNumber, maxPage)


        return StoryPage(
            storySlug = slug,
            pageNumber = pageNumber,
            content = content,
            totalPages = totalPages
        )
    }


    // ─── Listing Pages (New / Top / Category) ────────────────────────────────

    /**
     * Parses story card listings from /new/stories, /c/{slug}, /top/... pages.
     */
    fun parseStoryListing(html: String): List<Story> {
        val doc = Jsoup.parse(html)
        val stories = mutableListOf<Story>()

        // Story cards come in different containers depending on page type and site version (Beta vs Classic)
        val cards = doc.select("div.b-sli, div.panel-item, li.j_jk, div.story-card, div[data-id]")
        for (card in cards) {
            val story = parseStoryCard(card) ?: continue
            stories.add(story)
        }

        // Robust fallback: if no cards found with standard selectors, try to find any link matching the story pattern /s/
        if (stories.isEmpty()) {
            val storyLinks = doc.select("a[href*='/s/']").filter { 
                val href = it.attr("href")
                href.contains("/s/") && !href.contains("/series/") 
            }
            for (link in storyLinks) {
                val slug = link.attr("href").substringAfterLast("/s/").substringBefore("?").trim('/')
                if (slug.isNotBlank() && stories.none { it.slug == slug }) {
                    stories.add(Story(
                        slug = slug,
                        title = link.text().trim(),
                        author = Author(username = ""),
                        category = Category(slug = "", name = "")
                    ))
                }
            }
        }
        return stories
    }

    private fun parseStoryCard(card: org.jsoup.nodes.Element): Story? {
        val titleEl = card.selectFirst("a.j_jj, .b-sli-title a, h2 a, a.r-34i, .sl-title a, a[href*='/s/']") ?: return null
        val href = titleEl.attr("abs:href").ifBlank { titleEl.attr("href") }
        val slug = href.substringAfterLast("/s/").substringBefore("?").trim('/')
        if (slug.isBlank() || slug == "stories") return null
        val title = titleEl.text().trim()

        // Author
        val authorEl = card.selectFirst("a.y_eU, .b-sli-info a, span.y_eU a")
        val authorUsername = authorEl?.attr("href")
            ?.substringAfterLast("/authors/")?.trimEnd('/') ?: ""
        val authorName = authorEl?.text() ?: authorUsername

        // Category
        val catEl = card.selectFirst("a.y_eS, .b-sli-meta-cat a")
        val catName = catEl?.text() ?: ""
        val catSlug = catEl?.attr("href")?.substringAfterLast("/c/")?.trimEnd('/') ?: ""

        // Rating
        var rating = card.selectFirst("span.b-r-value, span.j_ra")?.text()?.toFloatOrNull() ?: 0f
        if (rating == 0f) {
            rating = Regex("\\b([1-4]\\.[0-9]{2})\\b").find(card.text())?.value?.toFloatOrNull() ?: 0f
        }

        // Description/tagline
        val description = card.selectFirst("div.b-sli-description, div.j_jt")?.text() ?: ""

        // Date
        val date = card.selectFirst("span.j_da, span.b-sli-meta-date")?.text() ?: ""

        return Story(
            slug = slug,
            title = title,
            description = description,
            author = Author(username = authorUsername, displayName = authorName),
            category = Category(slug = catSlug, name = catName),
            rating = rating,
            datePublished = date
        )
    }

    /**
     * Determines how many pages a listing has by inspecting pagination links.
     */
    fun parseTotalListingPages(html: String): Int {
        val doc = Jsoup.parse(html)
        val lastPage = doc.select("div.b-pager-pages a, .l_pgs a").lastOrNull()?.text()?.trim()?.toIntOrNull()
        return lastPage ?: 1
    }

    // ─── Author Profile Page ─────────────────────────────────────────────────

    fun parseAuthorProfile(html: String, username: String): Author {
        val doc = Jsoup.parse(html)

        val displayName = doc.selectFirst("h1.j_pu, h1.b-description-title")?.text() ?: username
        val bio = doc.selectFirst("div.b-description-text, div.j_bi")?.text() ?: ""
        val avatarUrl = doc.selectFirst("img.j_au, img.b-description-avatar")?.attr("abs:src") ?: ""

        val followerText = doc.selectFirst("span.b-author-info-followers, span.j_fwc")?.text() ?: "0"
        val followerCount = followerText.replace(",", "").filter { it.isDigit() }.toIntOrNull() ?: 0

        val storyText = doc.selectFirst("span.b-author-info-stories, span.j_stc")?.text() ?: "0"
        val storyCount = storyText.filter { it.isDigit() }.toIntOrNull() ?: 0

        return Author(
            username = username,
            displayName = displayName,
            bio = bio,
            avatarUrl = avatarUrl,
            followerCount = followerCount,
            storyCount = storyCount
        )
    }

    fun parseAuthorStories(html: String): List<Story> = parseStoryListing(html)

    // ─── Series Page ─────────────────────────────────────────────────────────

    fun parseSeries(html: String, seriesId: String): Series {
        val doc = Jsoup.parse(html)
        val title = doc.selectFirst("h1")?.text() ?: "Unknown Series"

        val authorEl = doc.selectFirst("a.y_eU, .b-description-sub a")
        val authorUsername = authorEl?.attr("href")
            ?.substringAfterLast("/authors/")?.trimEnd('/') ?: ""

        val chapters = parseStoryListing(html)
        return Series(
            id = seriesId,
            title = title,
            author = Author(username = authorUsername),
            chapterCount = chapters.size,
            chapters = chapters
        )
    }

    // ─── Tag Page ────────────────────────────────────────────────────────────

    fun parseTagStories(html: String): List<Story> = parseStoryListing(html)
}
