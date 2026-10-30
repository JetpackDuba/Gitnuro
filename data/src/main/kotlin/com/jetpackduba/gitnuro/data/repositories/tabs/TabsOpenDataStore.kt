package com.jetpackduba.gitnuro.data.repositories.tabs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow

val tabsOpenPreference = stringPreferencesKey("tabs_open")
val selectedTabIndexPreference = intPreferencesKey("selected_tab")
val tabGroupsPreference = stringPreferencesKey("tab_groups")
val activeTabGroupPreference = stringPreferencesKey("active_tab_group")

class TabsOpenDataStore(
    private val dataStore: DataStore<Preferences>
) : DataStore<Preferences> by dataStore, Flow<Preferences> by dataStore.data
