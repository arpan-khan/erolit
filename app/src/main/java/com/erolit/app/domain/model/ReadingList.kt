package com.erolit.app.domain.model

import java.util.UUID

data class ReadingList(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val storyCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val stories: List<Story> = emptyList()
)
