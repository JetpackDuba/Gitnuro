package com.jetpackduba.gitnuro.di.modules

import com.jetpackduba.gitnuro.di.TabScope
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Module
class TabModule {
    @TabScope
    @Provides
    fun provideScope() = CoroutineScope(SupervisorJob())
}
