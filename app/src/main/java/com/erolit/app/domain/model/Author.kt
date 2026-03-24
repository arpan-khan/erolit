package com.erolit.app.domain.model

data class Author(
    val username: String,
    val displayName: String = username,
    val avatarUrl: String = "",
    val bio: String = "",
    val followerCount: Int = 0,
    val storyCount: Int = 0,
    val isFollowing: Boolean = false
) {
    val profileUrl: String get() = "https://www.literotica.com/authors/$username"
    val storiesUrl: String get() = "https://www.literotica.com/authors/$username/works/stories"
}
