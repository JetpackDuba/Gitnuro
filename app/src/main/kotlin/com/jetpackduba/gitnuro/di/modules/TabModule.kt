package com.jetpackduba.gitnuro.di.modules

import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Module
class TabModule {
    @TabScope
    @Provides
    fun provideScope() = CoroutineScope(SupervisorJob())
    @TabScope
    @Provides
    fun provideTabCoroutineScope() = TabCoroutineScope()
}
