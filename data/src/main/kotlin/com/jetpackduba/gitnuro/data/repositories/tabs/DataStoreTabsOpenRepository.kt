package com.jetpackduba.gitnuro.data.repositories.tabs

import com.jetpackduba.gitnuro.data.extensions.get
import com.jetpackduba.gitnuro.data.extensions.setValue
import com.jetpackduba.gitnuro.data.mappers.PathsListMapper
import com.jetpackduba.gitnuro.domain.repositories.TabsOpenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
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

    override val groups: Flow<Map<String, List<String>>> = dataStore[tabGroupsPreference]
        .map { it?.let { json.decodeFromString<Map<String, List<String>>>(it) }.orEmpty() }

    override val activeGroup: Flow<String> = dataStore[activeTabGroupPreference]
        .map { it ?: DEFAULT_GROUP_NAME }

    override suspend fun updateOpenTabs(tabs: List<String>) {
        dataStore.setValue(tabsOpenPreference, pathsListMapper.toData(tabs))
    }

    override suspend fun updateSelectedTabIndex(value: Int) {
        dataStore.setValue(selectedTabIndexPreference, value)
    }

    override suspend fun updateGroups(groups: Map<String, List<String>>) {
        dataStore.setValue(tabGroupsPreference, json.encodeToString(groups))
    }

    override suspend fun updateActiveGroup(name: String) {
        dataStore.setValue(activeTabGroupPreference, name)
    }

    private companion object {
        const val DEFAULT_GROUP_NAME = "Default"
        val json = Json { ignoreUnknownKeys = true }
    }
}
