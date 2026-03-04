package com.jetpackduba.gitnuro.di.modules

import com.jetpackduba.gitnuro.common.OS
import com.jetpackduba.gitnuro.common.currentOs
import com.jetpackduba.gitnuro.domain.credentials.external.IGitCredentialsManagerProvider
import com.jetpackduba.gitnuro.domain.credentials.external.NixGitCredentialsManagerProvider
import com.jetpackduba.gitnuro.domain.credentials.external.WindowsGitCredentialsManagerProvider
import dagger.Module
import dagger.Provides
import javax.inject.Provider

@Module
class GitCredentialsManagerModule {
    @Provides
    fun providesGitCredentialsManagerProvider(
        windowsGitCredentialsManagerProvider: Provider<WindowsGitCredentialsManagerProvider>,
        nixGitCredentialsManagerProvider: Provider<NixGitCredentialsManagerProvider>,
    ): IGitCredentialsManagerProvider {
        return when (currentOs) {
            OS.LINUX, OS.MAC ->  nixGitCredentialsManagerProvider.get() // TODO Test this on MacOs
            OS.WINDOWS -> windowsGitCredentialsManagerProvider.get()
            OS.UNKNOWN -> throw IllegalStateException("Unknown OS")
        }
    }
}