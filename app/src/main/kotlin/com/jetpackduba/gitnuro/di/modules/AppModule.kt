package com.jetpackduba.gitnuro.di.modules

import com.jetpackduba.gitnuro.di.qualifiers.AppCoroutineScope
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.skiko.ClipboardManager

@Module
class AppModule {
    @Provides
    @AppCoroutineScope
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    fun provideClipboardManager(): ClipboardManager = ClipboardManager()
}