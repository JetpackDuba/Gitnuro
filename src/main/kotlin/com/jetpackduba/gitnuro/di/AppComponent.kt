package com.jetpackduba.gitnuro.di

import com.jetpackduba.gitnuro.App
import com.jetpackduba.gitnuro.AppEnvInfo
import com.jetpackduba.gitnuro.AppStateManager
import com.jetpackduba.gitnuro.credentials.CredentialsStateManager
import com.jetpackduba.gitnuro.di.modules.AppModule
import com.jetpackduba.gitnuro.preferences.AppSettings
import com.jetpackduba.gitnuro.viewmodels.SettingsViewModel
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class
    ]
)
interface AppComponent {
    fun inject(main: App)
    fun appStateManager(): AppStateManager
    fun settingsViewModel(): SettingsViewModel
    fun credentialsStateManager(): CredentialsStateManager

    fun appPreferences(): AppSettings

    fun appEnvInfo(): AppEnvInfo
}