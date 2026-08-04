package com.cloudlink.app.di

import com.cloudlink.app.data.repository.LogRepositoryImpl
import com.cloudlink.app.data.repository.ServerRepositoryImpl
import com.cloudlink.app.data.repository.SnippetRepositoryImpl
import com.cloudlink.app.domain.repository.LogRepository
import com.cloudlink.app.domain.repository.ServerRepository
import com.cloudlink.app.domain.repository.SnippetRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindServerRepository(
        serverRepositoryImpl: ServerRepositoryImpl
    ): ServerRepository

    @Binds
    @Singleton
    abstract fun bindSnippetRepository(
        snippetRepositoryImpl: SnippetRepositoryImpl
    ): SnippetRepository

    @Binds
    @Singleton
    abstract fun bindLogRepository(
        logRepositoryImpl: LogRepositoryImpl
    ): LogRepository
}
