package com.erolit.app.domain.model

data class StoryPage(
    val storySlug: String,
    val pageNumber: Int,
    val content: String,       // plain text, HTML stripped
    val totalPages: Int = 1
)
