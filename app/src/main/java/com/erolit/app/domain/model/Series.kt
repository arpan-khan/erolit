package com.erolit.app.domain.model

data class Series(
    val id: String,
    val title: String,
    val author: Author,
    val description: String = "",
    val chapterCount: Int = 0,
    val chapters: List<Story> = emptyList(),
    val lastUpdated: String = ""
) {
    val url: String get() = "https://www.literotica.com/series/se/$id"
}
