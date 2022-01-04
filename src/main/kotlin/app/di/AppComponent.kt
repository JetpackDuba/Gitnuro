package app.di

import app.AppStateManager
import app.App
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component
interface AppComponent {
    fun inject(main: App)
    fun appStateManager(): AppStateManager
}