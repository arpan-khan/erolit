package com.erolit.app.domain.repository

import com.erolit.app.domain.model.Author
import com.erolit.app.domain.model.Story
import kotlinx.coroutines.flow.Flow

interface AuthorRepository {
    suspend fun getAuthor(username: String): Result<Author>
    fun getAuthorStories(username: String): Flow<Result<List<Story>>>
}
