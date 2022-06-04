package app.di

import app.App
import app.AppPreferences
import app.AppStateManager
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component
interface AppComponent {
    fun inject(main: App)
    fun appStateManager(): AppStateManager

    fun appPreferences(): AppPreferences
}