package dev.erolit.reader.ui.navigation

import android.net.Uri

object Routes {
    const val Home = "home"
    const val Categories = "categories"
    const val Search = "search?q={q}"
    const val Lists = "lists"
    const val Settings = "settings"

    const val Story = "story/{storyId}"
    const val Series = "series/{seriesId}"
    const val Author = "author/{authorId}"
    const val Category = "category/{categoryId}/{name}"
    const val ListDetail = "list/{urlname}"
    const val Web = "web?url={url}&title={title}"

    fun story(id: Long) = "story/$id"
    fun series(id: Long) = "series/$id"
    fun author(id: Long) = "author/$id"
    fun category(id: Int, name: String) = "category/$id/${Uri.encode(name)}"
    fun list(urlname: String) = "list/${Uri.encode(urlname)}"
    fun search(query: String? = null) =
        if (query.isNullOrBlank()) "search" else "search?q=${Uri.encode(query)}"
    fun web(url: String, title: String? = null): String {
        val base = "web?url=${Uri.encode(url)}"
        return if (title.isNullOrBlank()) base else "$base&title=${Uri.encode(title)}"
    }
}
