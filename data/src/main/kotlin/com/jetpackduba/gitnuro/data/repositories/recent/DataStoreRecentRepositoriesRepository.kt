package com.jetpackduba.gitnuro.data.repositories.recent

import com.jetpackduba.gitnuro.data.extensions.get
import com.jetpackduba.gitnuro.data.extensions.setValue
import com.jetpackduba.gitnuro.domain.repositories.RecentRepositoriesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreRecentRepositoriesRepository @Inject constructor(
    private val dataStore: RecentRepositoriesDataStore,
) : RecentRepositoriesRepository {
    override val recentRepositoriesPaths: Flow<Set<String>> = dataStore[recentRepositoriesPreference].map { it.orEmpty() }

    override suspend fun updateRecentRepository(repositoriesPaths: Set<String>) {
        dataStore.setValue(recentRepositoriesPreference, repositoriesPaths)
    }
}