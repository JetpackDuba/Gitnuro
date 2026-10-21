package com.jetpackduba.gitnuro.di.modules

import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import dagger.Module
import dagger.Provides

@Module
class TabModule {    @TabScope
    @Provides
    fun provideTabCoroutineScope() = TabCoroutineScope()
}
