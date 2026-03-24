package com.erolit.app.di

import com.erolit.app.data.repository.AuthorRepositoryImpl
import com.erolit.app.data.repository.LibraryRepositoryImpl
import com.erolit.app.data.repository.SearchRepositoryImpl
import com.erolit.app.data.repository.StoryRepositoryImpl
import com.erolit.app.domain.repository.AuthorRepository
import com.erolit.app.domain.repository.LibraryRepository
import com.erolit.app.domain.repository.SearchRepository
import com.erolit.app.domain.repository.StoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindStoryRepository(impl: StoryRepositoryImpl): StoryRepository

    @Binds @Singleton
    abstract fun bindAuthorRepository(impl: AuthorRepositoryImpl): AuthorRepository

    @Binds @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    @Binds @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository
}
