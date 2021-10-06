package app.di

import app.Main
import dagger.Component

@Component
interface AppComponent {
    fun inject(main: Main)
}