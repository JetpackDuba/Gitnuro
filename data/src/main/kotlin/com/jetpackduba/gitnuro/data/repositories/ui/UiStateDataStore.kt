package com.jetpackduba.gitnuro.data.repositories.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow

val windowPlacementPreference get() = intPreferencesKey("window_placement")
val firstPaneWidthPreference get() = floatPreferencesKey("first_pane_width")
val thirdPaneWidthPreference get() = floatPreferencesKey("third_pane_width")

class UiStateDataStore(
    private val dataStore: DataStore<Preferences>
) : DataStore<Preferences> by dataStore, Flow<Preferences> by dataStore.data
