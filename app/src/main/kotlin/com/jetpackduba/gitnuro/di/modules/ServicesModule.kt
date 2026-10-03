package com.jetpackduba.gitnuro.di.modules

import com.jetpackduba.gitnuro.data.services.GitProviderService
import com.jetpackduba.gitnuro.domain.services.IGitProviderService
import dagger.Binds
import dagger.Module

@Module
interface ServicesModule {
    @Binds
    fun bindIGitProviderService(service: GitProviderService): IGitProviderService

}