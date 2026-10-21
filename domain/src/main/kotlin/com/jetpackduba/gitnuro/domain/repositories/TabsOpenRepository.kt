package com.jetpackduba.gitnuro.domain.repositories

import kotlinx.coroutines.flow.Flow

interface TabsOpenRepository {
    val openTabs: Flow<List<String>>
    val selectedTabIndex: Flow<Int?>

    suspend fun updateOpenTabs(tabs: List<String>)
    suspend fun updateSelectedTabIndex(value: Int)
}