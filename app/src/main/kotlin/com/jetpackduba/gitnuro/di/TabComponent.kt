package com.jetpackduba.gitnuro.di

import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.data.di.TabRepositoriesModule
import com.jetpackduba.gitnuro.di.modules.TabModule
import com.jetpackduba.gitnuro.ui.components.TabInformation
import dagger.Component

@TabScope
@Component(
    modules = [
        TabModule::class,
        TabRepositoriesModule::class,
    ],
    dependencies = [
        AppComponent::class
    ],
)
interface TabComponent {
    fun tabInformationFactory(): TabInformation.Factory
}