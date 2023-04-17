package com.jetpackduba.gitnuro.di

import com.jetpackduba.gitnuro.di.modules.NetworkModule
import com.jetpackduba.gitnuro.di.modules.ShellModule
import com.jetpackduba.gitnuro.di.modules.TabModule
import com.jetpackduba.gitnuro.ui.components.TabInformation
import dagger.Component

@TabScope
@Component(
    modules = [
        NetworkModule::class,
        TabModule::class,
    ],
    dependencies = [
        AppComponent::class
    ],
)
interface TabComponent {
    fun inject(tabInformation: TabInformation)
}