package com.jetpackduba.gitnuro.di

import com.jetpackduba.gitnuro.App
import com.jetpackduba.gitnuro.data.di.DatastoreModule
import com.jetpackduba.gitnuro.di.modules.*
import dagger.Component
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
}
