package com.erolit.app.domain.model

data class Tag(
    val name: String,
    val storyCount: Int = 0
) {
    val url: String get() = "https://tags.literotica.com/$name/"
}
