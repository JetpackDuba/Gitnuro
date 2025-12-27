package com.jetpackduba.gitnuro.di

import com.jetpackduba.gitnuro.App
import com.jetpackduba.gitnuro.AppEnvInfo
import com.jetpackduba.gitnuro.credentials.CredentialsCacheRepository
import com.jetpackduba.gitnuro.credentials.CredentialsStateManager
import com.jetpackduba.gitnuro.credentials.external.IGitCredentialsManagerProvider
import com.jetpackduba.gitnuro.di.modules.AppModule
import com.jetpackduba.gitnuro.di.modules.GitCredentialsManagerModule
import com.jetpackduba.gitnuro.di.modules.NetworkModule
import com.jetpackduba.gitnuro.di.modules.ShellModule
import com.jetpackduba.gitnuro.managers.AppStateManager
import com.jetpackduba.gitnuro.managers.IShellManager
import com.jetpackduba.gitnuro.managers.TempFilesManager
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
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
    ]
)
interface AppComponent {
    fun inject(main: App)
    fun appStateManager(): AppStateManager
    fun settingsViewModel(): SettingsViewModel
    fun credentialsStateManager(): CredentialsStateManager

    fun appPreferences(): AppSettingsRepository

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
}
