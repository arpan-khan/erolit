package com.erolit.app.data.repository

import com.erolit.app.data.local.dao.StoryDao
import com.erolit.app.data.remote.LiteroticaDataSource
import com.erolit.app.domain.model.Author
import com.erolit.app.domain.model.Story
import com.erolit.app.domain.repository.AuthorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthorRepositoryImpl @Inject constructor(
    private val dataSource: LiteroticaDataSource
) : AuthorRepository {

    override suspend fun getAuthor(username: String): Result<Author> =
        runCatching { dataSource.getAuthor(username) }

    override fun getAuthorStories(username: String): Flow<Result<List<Story>>> = flow {
        runCatching { dataSource.getAuthorStories(username) }
            .onSuccess { emit(Result.success(it)) }
            .onFailure { emit(Result.failure(it)) }
    }
}
