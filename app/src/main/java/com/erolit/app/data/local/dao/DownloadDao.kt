package com.erolit.app.data.local.dao

import androidx.room.*
import com.erolit.app.data.local.entity.DownloadedPageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: DownloadedPageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<DownloadedPageEntity>)

    @Query("SELECT * FROM downloaded_pages WHERE storySlug = :slug ORDER BY pageNumber ASC")
    suspend fun getPagesForStory(slug: String): List<DownloadedPageEntity>

    @Query("SELECT COUNT(*) FROM downloaded_pages WHERE storySlug = :slug")
    suspend fun getPageCount(slug: String): Int

    @Query("DELETE FROM downloaded_pages WHERE storySlug = :slug")
    suspend fun deleteStoryPages(slug: String)
}
