package com.jetpackduba.gitnuro.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.jetpackduba.gitnuro.data.UserSettingsDataStore
import com.jetpackduba.gitnuro.data.repositories.getPreferencesPath
import dagger.Module
import dagger.Provides
import okio.Path.Companion.toPath
import javax.inject.Singleton

@Module
class DatastoreModule {
    @Singleton
    @Provides
    fun provideDataStore(): UserSettingsDataStore {
        val preferences = PreferenceDataStoreFactory.createWithPath(
            produceFile = { getPreferencesPath().toPath() }
        )

        return UserSettingsDataStore(preferences)
    }
}