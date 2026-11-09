package dev.erolit.reader.core.net

import okhttp3.HttpUrl.Companion.toHttpUrl

data class ApiConfig(
    val apiBase: String = "https://literotica.com/api/3",
    val authBase: String = "https://auth.literotica.com",
    val wwwBase: String = "https://www.literotica.com",
)

/**
 * The upstream base host is intentionally not shown in the UI. All paths are
 * centralised here so a routing change can be handled in one place.
 */
class ApiRoutes(private val config: ApiConfig) {
    fun url(path: String, paramsJson: String? = null): String {
        val base = if (path.startsWith("http")) path else config.apiBase + path
        val builder = base.toHttpUrl().newBuilder()
        if (paramsJson != null) builder.addQueryParameter("params", paramsJson)
        return builder.build().toString()
    }

    fun categories(): String = url("/tagsportal/categories")
    fun newStories(paramsJson: String): String = url("/stories/new", paramsJson)
    fun topRated(paramsJson: String): String = url("/stories/toprated", paramsJson)
    fun mostRead(paramsJson: String): String = url("/stories/mostread", paramsJson)
    fun topFavorited(paramsJson: String): String = url("/stories/topfavorited", paramsJson)
    fun browse(paramsJson: String): String = url("/tagsportal/stories", paramsJson)
    fun story(idOrSlug: String, paramsJson: String): String = url("/stories/$idOrSlug", paramsJson)
    fun series(idOrSlug: String): String = url("/series/$idOrSlug")
    fun seriesWorks(idOrSlug: String): String = url("/series/$idOrSlug/works")
    fun seriesFeed(feed: String, paramsJson: String? = null): String =
        url("/series/$feed", paramsJson)
    fun author(id: Long): String = url("/users/$id")
    fun authorStories(id: Long, paramsJson: String): String = url("/users/$id/stories", paramsJson)
    fun authorLists(id: Long): String = url("/users/$id/lists")
    fun searchStories(paramsJson: String): String = url("/search/stories", paramsJson)
    fun searchPoems(paramsJson: String): String = url("/search/poems", paramsJson)
    fun searchMembers(paramsJson: String): String = url("/search/members", paramsJson)
    fun myLists(): String = url("/my/lists")
    fun myList(urlname: String, paramsJson: String): String = url("/my/lists/$urlname", paramsJson)
    fun createList(userId: Long): String = url("/users/$userId/lists")
    fun deleteList(listId: Long): String = url("/lists/$listId")
    fun addStoryToList(storyId: Long, listId: Long): String = url("/stories/$storyId/lists/$listId")
    fun storiesLists(storyId: Long): String = url("/stories/$storyId/lists")
    fun session(): String = url("/users/session")

    fun loginUrl(): String = (config.authBase + "/login").toHttpUrl().newBuilder()
        .addQueryParameter("redirect", "/")
        .addQueryParameter("err_redirect", config.wwwBase + "/authenticate/login")
        .build()
        .toString()

    fun loginFormUrl(): String = config.wwwBase + "/authenticate/login"

    fun checkUrl(timestampSeconds: Long): String = (config.authBase + "/check").toHttpUrl().newBuilder()
        .addQueryParameter("timestamp", timestampSeconds.toString())
        .build()
        .toString()

    fun logoutUrl(): String = config.authBase + "/session/logout"

    fun shareStoryUrl(slug: String): String = config.wwwBase + "/s/" + slug

    fun loginOrigin(): String = config.wwwBase
}
