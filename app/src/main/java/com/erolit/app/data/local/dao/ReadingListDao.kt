package com.erolit.app.data.local.dao

import androidx.room.*
import com.erolit.app.data.local.entity.ReadingListEntity
import com.erolit.app.data.local.entity.ReadingListStoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingListDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ReadingListEntity)

    @Delete
    suspend fun deleteList(list: ReadingListEntity)

    @Query("SELECT * FROM reading_lists ORDER BY createdAt DESC")
    fun getAllLists(): Flow<List<ReadingListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addStoryToList(entity: ReadingListStoryEntity)

    @Query("DELETE FROM reading_list_stories WHERE listId = :listId AND storySlug = :storySlug")
    suspend fun removeStoryFromList(listId: String, storySlug: String)

    @Query("SELECT storySlug FROM reading_list_stories WHERE listId = :listId ORDER BY addedAt DESC")
    fun getStorySlugsForList(listId: String): Flow<List<String>>
}
