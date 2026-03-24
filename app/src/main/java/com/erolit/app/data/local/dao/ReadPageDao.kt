package com.erolit.app.data.local.dao

import androidx.room.*
import com.erolit.app.data.local.entity.ReadPageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadPageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markAsRead(readPage: ReadPageEntity)

    @Delete
    suspend fun markAsUnread(readPage: ReadPageEntity)

    @Query("SELECT pageNumber FROM read_pages WHERE slug = :slug")
    fun getReadPages(slug: String): Flow<List<Int>>

    @Query("DELETE FROM read_pages WHERE slug = :slug")
    suspend fun clearReadStatus(slug: String)
}
