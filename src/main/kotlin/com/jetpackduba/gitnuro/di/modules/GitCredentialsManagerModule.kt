package com.jetpackduba.gitnuro.di.modules

import com.jetpackduba.gitnuro.credentials.external.IGitCredentialsManagerProvider
import com.jetpackduba.gitnuro.credentials.external.WindowsGitCredentialsManagerProvider
import com.jetpackduba.gitnuro.system.OS
import com.jetpackduba.gitnuro.system.currentOs
import dagger.Module
import dagger.Provides
import javax.inject.Provider

@Module
class GitCredentialsManagerModule {
    @Provides
    fun providesGitCredentialsManagerProvider(
        windowsGitCredentialsManagerProvider: Provider<WindowsGitCredentialsManagerProvider>,
    ): IGitCredentialsManagerProvider {
        return when (currentOs) {
            OS.LINUX -> TODO()
            OS.WINDOWS -> windowsGitCredentialsManagerProvider.get()
            OS.MAC -> TODO()
            OS.UNKNOWN -> TODO()
        }
    }
}