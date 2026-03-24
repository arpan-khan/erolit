package com.erolit.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val slug: String,
    val title: String,
    val authorUsername: String,
    val authorDisplayName: String,
    val authorAvatarUrl: String,
    val categorySlug: String,
    val categoryName: String,
    val description: String,
    val rating: Float,
    val ratingCount: Int,
    val commentCount: Int,
    val favoriteCount: Int,
    val wordCount: Int,
    val pageCount: Int,
    val tags: String,           // comma-separated
    val datePublished: String,
    val isSaved: Boolean = false,
    val isDownloaded: Boolean = false,
    val savedAt: Long = 0L,
    val lastReadAt: Long = 0L,
    val readingProgress: Float = 0f,
    val lastPageRead: Int = 1
)
