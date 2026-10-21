package com.jetpackduba.gitnuro.data.repositories.tabs

import com.jetpackduba.gitnuro.data.extensions.get
import com.jetpackduba.gitnuro.data.extensions.setValue
import com.jetpackduba.gitnuro.data.mappers.PathsListMapper
import com.jetpackduba.gitnuro.domain.repositories.TabsOpenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreTabsOpenRepository @Inject constructor(
    private val dataStore: TabsOpenDataStore,
    private val pathsListMapper: PathsListMapper,
) : TabsOpenRepository {
    override val openTabs: Flow<List<String>> = dataStore[tabsOpenPreference]
        .map {
            it?.let { pathsListMapper.toDomain(it) }.orEmpty()
        }

    override val selectedTabIndex: Flow<Int?> = dataStore[selectedTabIndexPreference]

    override suspend fun updateOpenTabs(tabs: List<String>) {
        dataStore.setValue(tabsOpenPreference, pathsListMapper.toData(tabs))
    }

    override suspend fun updateSelectedTabIndex(value: Int) {
        dataStore.setValue(selectedTabIndexPreference, value)
    }
}