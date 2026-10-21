package com.jetpackduba.gitnuro.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

class GlobalSettingsDataStore(
    private val dataStore: DataStore<Preferences>
) : DataStore<Preferences> by dataStore, Flow<Preferences> by dataStore.data