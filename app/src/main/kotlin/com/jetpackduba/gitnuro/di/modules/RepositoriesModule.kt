package com.jetpackduba.gitnuro.di.modules

import com.jetpackduba.gitnuro.data.repositories.CredentialsCacheRepository
import com.jetpackduba.gitnuro.data.repositories.InMemoryNotificationsRepository
import com.jetpackduba.gitnuro.data.repositories.JvmSystemProxyRepository
import com.jetpackduba.gitnuro.data.repositories.NetworkLfsRepository
import com.jetpackduba.gitnuro.data.repositories.SkikoClipboardRepository
import com.jetpackduba.gitnuro.domain.repositories.AppSettingsRepository
import com.jetpackduba.gitnuro.data.repositories.configuration.DataStoreAppSettingsRepository
import com.jetpackduba.gitnuro.domain.repositories.ClipboardRepository
import com.jetpackduba.gitnuro.domain.repositories.CredentialsRepository
import com.jetpackduba.gitnuro.domain.repositories.LfsRepository
import com.jetpackduba.gitnuro.domain.repositories.NotificationsRepository
import com.jetpackduba.gitnuro.domain.repositories.SystemProxyRepository
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
interface RepositoriesModule {
    @Binds
    fun clipboardRepository(repository: SkikoClipboardRepository): ClipboardRepository

    @Binds
    fun notificationsRepository(repository: InMemoryNotificationsRepository): NotificationsRepository

    @Binds
    fun systemProxyRepository(repository: JvmSystemProxyRepository): SystemProxyRepository

    @Singleton
    @Binds
    fun appSettingsRepository(repository: DataStoreAppSettingsRepository): AppSettingsRepository

    @Singleton
    @Binds
    fun credentialsRepository(repository: CredentialsCacheRepository): CredentialsRepository

    @Binds
    fun lfsRepository(repository: NetworkLfsRepository): LfsRepository
}