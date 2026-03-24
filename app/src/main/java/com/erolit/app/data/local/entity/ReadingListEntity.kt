package com.erolit.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_lists")
data class ReadingListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val createdAt: Long
)

@Entity(
    tableName = "reading_list_stories",
    primaryKeys = ["listId", "storySlug"]
)
data class ReadingListStoryEntity(
    val listId: String,
    val storySlug: String,
    val addedAt: Long = System.currentTimeMillis()
)
