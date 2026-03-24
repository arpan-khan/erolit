package com.erolit.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "downloaded_pages",
    indices = [androidx.room.Index(value = ["storySlug", "pageNumber"], unique = true)]
)
data class DownloadedPageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val storySlug: String,
    val pageNumber: Int,
    val content: String,
    val totalPages: Int,
    val downloadedAt: Long = System.currentTimeMillis()
)
