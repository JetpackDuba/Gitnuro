package com.jetpackduba.gitnuro.di.modules

import com.jetpackduba.gitnuro.data.repositories.CredentialsCacheRepository
import com.jetpackduba.gitnuro.data.repositories.InMemoryNotificationsRepository
import com.jetpackduba.gitnuro.data.repositories.JvmSystemProxyRepository
import com.jetpackduba.gitnuro.data.repositories.NetworkLfsRepository
import com.jetpackduba.gitnuro.domain.repositories.GlobalSettingsRepository
import com.jetpackduba.gitnuro.data.repositories.configuration.DataStoreGlobalSettingsRepository
import com.jetpackduba.gitnuro.data.repositories.recent.DataStoreRecentRepositoriesRepository
import com.jetpackduba.gitnuro.data.repositories.tabs.DataStoreTabsOpenRepository
import com.jetpackduba.gitnuro.data.repositories.ui.DataStoreUiStateRepository
import com.jetpackduba.gitnuro.domain.repositories.CredentialsRepository
import com.jetpackduba.gitnuro.domain.repositories.LfsRepository
import com.jetpackduba.gitnuro.domain.repositories.NotificationsRepository
import com.jetpackduba.gitnuro.domain.repositories.RecentRepositoriesRepository
import com.jetpackduba.gitnuro.domain.repositories.SystemProxyRepository
import com.jetpackduba.gitnuro.domain.repositories.TabsOpenRepository
import com.jetpackduba.gitnuro.domain.repositories.UiStateRepository
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
interface RepositoriesModule {
    @Binds
    fun notificationsRepository(repository: InMemoryNotificationsRepository): NotificationsRepository

    @Binds
    fun systemProxyRepository(repository: JvmSystemProxyRepository): SystemProxyRepository

    @Singleton
    @Binds
    fun appSettingsRepository(repository: DataStoreGlobalSettingsRepository): GlobalSettingsRepository

    @Singleton
    @Binds
    fun credentialsRepository(repository: CredentialsCacheRepository): CredentialsRepository

    @Binds
    fun lfsRepository(repository: NetworkLfsRepository): LfsRepository

    @Singleton
    @Binds
    fun recentRepositoriesRepository(repository: DataStoreRecentRepositoriesRepository): RecentRepositoriesRepository

    @Singleton
    @Binds
    fun tabsOpenRepository(repository: DataStoreTabsOpenRepository): TabsOpenRepository

    @Singleton
    @Binds
    fun uiStateRepository(repository: DataStoreUiStateRepository): UiStateRepository
}