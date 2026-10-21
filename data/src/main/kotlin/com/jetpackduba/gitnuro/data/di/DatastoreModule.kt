package com.jetpackduba.gitnuro.data.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.jetpackduba.gitnuro.data.DataStoreFilesProvider
import com.jetpackduba.gitnuro.data.GlobalSettingsDataStore
import com.jetpackduba.gitnuro.data.repositories.recent.RecentRepositoriesDataStore
import com.jetpackduba.gitnuro.data.repositories.tabs.TabsOpenDataStore
import com.jetpackduba.gitnuro.data.repositories.ui.UiStateDataStore
import dagger.Module
import dagger.Provides
import okio.Path.Companion.toPath
import javax.inject.Singleton

@Module
class DatastoreModule {
    @Singleton
    @Provides
    fun provideGlobalSettingsDataStore(
        dataStoreFilesProvider: DataStoreFilesProvider
    ): GlobalSettingsDataStore {
        val preferences = PreferenceDataStoreFactory.createWithPath(
            produceFile = { dataStoreFilesProvider.getGlobalSettingsPath().toPath() }
        )

        return GlobalSettingsDataStore(preferences)
    }

    @Singleton
    @Provides
    fun provideRecentRepositoriesDataStore(
        dataStoreFilesProvider: DataStoreFilesProvider
    ): RecentRepositoriesDataStore {
        val preferences = PreferenceDataStoreFactory.createWithPath(
            produceFile = { dataStoreFilesProvider.getRecentRepositoriesPath().toPath() }
        )

        return RecentRepositoriesDataStore(preferences)
    }

    @Singleton
    @Provides
    fun provideTabsOpenDataStore(
        dataStoreFilesProvider: DataStoreFilesProvider
    ): TabsOpenDataStore {
        val preferences = PreferenceDataStoreFactory.createWithPath(
            produceFile = { dataStoreFilesProvider.getTabsOpenPath().toPath() }
        )

        return TabsOpenDataStore(preferences)
    }

    @Singleton
    @Provides
    fun provideUiSizesDataStore(
        dataStoreFilesProvider: DataStoreFilesProvider
    ): UiStateDataStore {
        val preferences = PreferenceDataStoreFactory.createWithPath(
            produceFile = { dataStoreFilesProvider.getUiSizesPath().toPath() }
        )

        return UiStateDataStore(preferences)
    }
}