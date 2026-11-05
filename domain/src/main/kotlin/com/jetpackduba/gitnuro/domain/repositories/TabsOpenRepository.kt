package com.jetpackduba.gitnuro.domain.repositories

import kotlinx.coroutines.flow.Flow

interface TabsOpenRepository {
    val openTabs: Flow<List<String>>
    val selectedTabIndex: Flow<Int?>

    /** Saved repository paths per group. The entry of the active group may be stale, its live tabs are in [openTabs]. */
    val groups: Flow<Map<String, List<String>>>
    val activeGroup: Flow<String>

    suspend fun updateOpenTabs(tabs: List<String>)
    suspend fun updateSelectedTabIndex(value: Int)
    suspend fun updateGroups(groups: Map<String, List<String>>)
    suspend fun updateActiveGroup(name: String)
}