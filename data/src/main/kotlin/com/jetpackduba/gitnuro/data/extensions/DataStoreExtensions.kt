package com.jetpackduba.gitnuro.data.extensions

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

operator fun <T> Flow<Preferences>.get(key: Preferences.Key<T>): Flow<T?> {
    return this.map { it[key] }
}

suspend fun <T> DataStore<Preferences>.setValue(key: Preferences.Key<T>, value: T?) {
    this.updateData {
        it.toMutablePreferences().also { preferences ->
            if (value == null) {
                preferences.remove(key)
            } else {
                preferences[key] = value
            }
        }
    }
}