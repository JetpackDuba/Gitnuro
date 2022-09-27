package com.jetpackduba.gitnuro.di

import com.jetpackduba.gitnuro.App
import com.jetpackduba.gitnuro.AppStateManager
import com.jetpackduba.gitnuro.preferences.AppSettings
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component
interface AppComponent {
    fun inject(main: App)
    fun appStateManager(): AppStateManager

    fun appPreferences(): AppSettings
}