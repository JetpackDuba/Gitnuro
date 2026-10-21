package com.jetpackduba.gitnuro.data.repositories.recent

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow

val recentRepositoriesPreference get() = stringSetPreferencesKey("latest_opened_repositories_path")

class RecentRepositoriesDataStore(
    private val dataStore: DataStore<Preferences>
) : DataStore<Preferences> by dataStore, Flow<Preferences> by dataStore.data
