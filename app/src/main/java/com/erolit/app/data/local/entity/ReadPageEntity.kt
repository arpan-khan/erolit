package com.erolit.app.data.local.entity

import androidx.room.Entity

@Entity(tableName = "read_pages", primaryKeys = ["slug", "pageNumber"])
data class ReadPageEntity(
    val slug: String,
    val pageNumber: Int,
    val timestamp: Long = System.currentTimeMillis()
)
