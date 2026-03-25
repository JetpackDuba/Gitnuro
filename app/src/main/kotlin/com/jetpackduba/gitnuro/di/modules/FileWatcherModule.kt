package com.jetpackduba.gitnuro.di.modules

import com.jetpackduba.gitnuro.data.git.FileChangesWatcher
import com.jetpackduba.gitnuro.domain.interfaces.IFileChangesWatcher
import dagger.Binds
import dagger.Module

@Module
interface FileWatcherModule {
    @Binds
    fun bindFileWatcher(watcher: FileChangesWatcher) : IFileChangesWatcher
}