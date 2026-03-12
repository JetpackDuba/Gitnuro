package com.jetpackduba.gitnuro.di

import com.jetpackduba.gitnuro.App
import com.jetpackduba.gitnuro.AppEnvInfo
import com.jetpackduba.gitnuro.data.di.DatastoreModule
import com.jetpackduba.gitnuro.di.modules.RepositoriesModule
import com.jetpackduba.gitnuro.data.repositories.configuration.DataStoreAppSettingsRepository
import com.jetpackduba.gitnuro.data.repositories.CredentialsCacheRepository
import com.jetpackduba.gitnuro.di.modules.AppModule
import com.jetpackduba.gitnuro.di.modules.FileWatcherModule
import com.jetpackduba.gitnuro.di.modules.GitActionsModule
import com.jetpackduba.gitnuro.di.modules.GitCredentialsManagerModule
import com.jetpackduba.gitnuro.di.modules.NetworkModule
import com.jetpackduba.gitnuro.di.modules.ShellModule
import com.jetpackduba.gitnuro.domain.IShellManager
import com.jetpackduba.gitnuro.domain.TempFilesManager
import com.jetpackduba.gitnuro.domain.credentials.CredentialsStateManager
import com.jetpackduba.gitnuro.domain.credentials.external.IGitCredentialsManagerProvider
import com.jetpackduba.gitnuro.domain.repositories.AppSettingsRepository
import com.jetpackduba.gitnuro.domain.repositories.CredentialsRepository
import com.jetpackduba.gitnuro.domain.repositories.LfsRepository
import com.jetpackduba.gitnuro.managers.AppStateManager
import com.jetpackduba.gitnuro.terminal.ITerminalProvider
import com.jetpackduba.gitnuro.ui.TabsManager
import com.jetpackduba.gitnuro.ui.VerticalSplitPaneConfig
import com.jetpackduba.gitnuro.updates.UpdatesRepository
import com.jetpackduba.gitnuro.viewmodels.SettingsViewModel
import dagger.Component
import org.jetbrains.skiko.ClipboardManager
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        ShellModule::class,
        NetworkModule::class,
        GitCredentialsManagerModule::class,
        RepositoriesModule::class,
        DatastoreModule::class,
        GitActionsModule::class,
    ]
)
interface AppComponent {
    fun app(): App
    fun tabComponentFactory(): TabComponent.Factory

    fun appStateManager(): AppStateManager
    fun settingsViewModel(): SettingsViewModel

    fun credentialsStateManager(): CredentialsStateManager

    fun appPreferences(): DataStoreAppSettingsRepository

    fun verticalSplitPaneConfig(): VerticalSplitPaneConfig

    fun appEnvInfo(): AppEnvInfo

    fun tabsManager(): TabsManager

    fun shellManager(): IShellManager

    fun terminalProvider(): ITerminalProvider

    fun gitCredentialsManagerProvider(): IGitCredentialsManagerProvider

    fun tempFilesManager(): TempFilesManager

    fun updatesRepository(): UpdatesRepository

    fun credentialsCacheRepository(): CredentialsCacheRepository

    fun clipboardManager(): ClipboardManager

    fun lfsRepository(): LfsRepository

    fun credentialsRepository(): CredentialsRepository
    fun appSettingsRepository(): AppSettingsRepository
}
