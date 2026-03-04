package com.jetpackduba.gitnuro.data.di

import dagger.Module
import dagger.Provides
import org.jetbrains.skiko.ClipboardManager

@Module
class ClipboardManagerModule {
    @Provides
    fun provideClipboardManager(): ClipboardManager = ClipboardManager()
}
