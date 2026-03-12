package com.jetpackduba.gitnuro.di

import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.data.di.TabRepositoriesModule
import com.jetpackduba.gitnuro.di.modules.FileWatcherModule
import com.jetpackduba.gitnuro.di.modules.GitActionsModule
import com.jetpackduba.gitnuro.di.modules.TabModule
import com.jetpackduba.gitnuro.ui.components.TabInformation
import dagger.Subcomponent

@TabScope
@Subcomponent(
    modules = [
        TabModule::class,
        TabRepositoriesModule::class,
        FileWatcherModule::class,
    ],
)
interface TabComponent {
    fun tabInformationFactory(): TabInformation.Factory

    @Subcomponent.Factory
    interface Factory {
        fun create(): TabComponent
    }
}