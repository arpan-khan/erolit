package com.erolit.app.domain.model

data class Story(
    val slug: String,
    val title: String,
    val author: Author,
    val category: Category,
    val description: String = "",
    val rating: Float = 0f,
    val ratingCount: Int = 0,
    val commentCount: Int = 0,
    val favoriteCount: Int = 0,
    val wordCount: Int = 0,
    val pageCount: Int = 1,
    val tags: List<String> = emptyList(),
    val datePublished: String = "",
    val isDownloaded: Boolean = false,
    val isSaved: Boolean = false,
    val readingProgress: Float = 0f  // 0.0 to 1.0
) {
    val url: String get() = "https://www.literotica.com/s/$slug"
}
