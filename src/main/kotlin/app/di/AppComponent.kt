package app.di

import app.AppStateManager
import app.Main
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component
interface AppComponent {
    fun inject(main: Main)
    fun appStateManager(): AppStateManager
}