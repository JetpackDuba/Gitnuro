package com.jetpackduba.gitnuro.di.modules

import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.data.repositories.ErrorsRepository
import com.jetpackduba.gitnuro.data.repositories.InMemoryRepositoryDataRepository
import com.jetpackduba.gitnuro.data.repositories.InMemoryRepositoryStateRepository
import com.jetpackduba.gitnuro.domain.repositories.IErrorsRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import dagger.Binds
import dagger.Module

@Module
interface TabRepositoriesModule {
    @TabScope
    @Binds
    fun errorsRepository(repository: ErrorsRepository): IErrorsRepository

    @TabScope
    @Binds
    fun statusRepository(repository: InMemoryRepositoryDataRepository): RepositoryDataRepository

    @TabScope
    @Binds
    fun repositoryStateRepository(repository: InMemoryRepositoryStateRepository): RepositoryStateRepository
}