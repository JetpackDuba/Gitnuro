package com.jetpackduba.gitnuro.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.jetpackduba.gitnuro.di.AppComponent
import com.jetpackduba.gitnuro.preferences.AppSettings
import com.jetpackduba.gitnuro.ui.components.TabInformation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TabsManager @Inject constructor(
    private val appSettings: AppSettings
) {
    lateinit var appComponent: AppComponent

    private val _tabsFlow = MutableStateFlow<List<TabInformation>>(emptyList())
    val tabsFlow: StateFlow<List<TabInformation>> = _tabsFlow

    private val _currentTab = MutableStateFlow<TabInformation?>(null)
    val currentTab: StateFlow<TabInformation?> = _currentTab

    fun loadPersistedTabs() {
        val repositoriesSaved = appSettings.latestTabsOpened

        val repositoriesList = if (repositoriesSaved.isNotEmpty())
            Json.decodeFromString<List<String>>(repositoriesSaved).map { path ->
                newAppTab(
                    path = path,
                )
            }
        else
            listOf()

        _tabsFlow.value = repositoriesList.ifEmpty { listOf(newAppTab()) }

        val latestSelectedTabPath = appSettings.latestRepositoryTabSelected

        val latestSelectedTab = repositoriesList.firstOrNull { it.path == latestSelectedTabPath }

        _currentTab.value = latestSelectedTab ?: _tabsFlow.value.first()
    }

    fun addNewTabFromPath(path: String, selectTab: Boolean, tabToBeReplaced: TabInformation? = null) {
        val newTab = newAppTab(
            tabName = mutableStateOf(""),
            path = path,
        )

        _tabsFlow.update {
            val newTabsList = it.toMutableList()

            if (tabToBeReplaced != null) {
                val index = newTabsList.indexOf(tabToBeReplaced)
                newTabsList[index] = newTab
            } else {
                newTabsList.add(newTab)
            }

            newTabsList
        }

        if (selectTab) {
            _currentTab.value = newTab
        }
    }

    fun selectTab(tab: TabInformation) {
        _currentTab.value = tab

        persistTabSelected(tab)
    }

    private fun persistTabSelected(tab: TabInformation) {
        appSettings.latestRepositoryTabSelected = tab.path.orEmpty()
    }

    fun closeTab(tab: TabInformation) {
        val tabsList = _tabsFlow.value.toMutableList()
        var newCurrentTab: TabInformation? = null

        if (currentTab.value == tab) {
            val index = tabsList.indexOf(tab)

            if (tabsList.count() == 1) {
                newCurrentTab = newAppTab()
            } else if (index > 0) {
                newCurrentTab = tabsList[index - 1]
            } else if (index == 0) {
                newCurrentTab = tabsList[1]
            }
        }

        tab.dispose()
        tabsList.remove(tab)

        if (newCurrentTab != null) {
            if (!tabsList.contains(newCurrentTab)) {
                tabsList.add(newCurrentTab)
            }

            _tabsFlow.value = tabsList
            _currentTab.value = newCurrentTab
        } else {
            _tabsFlow.value = tabsList
        }

        updatePersistedTabs()
    }

    private fun updatePersistedTabs() {
        val tabsPaths = tabsFlow.value
            .mapNotNull { it.path }

        appSettings.latestTabsOpened = Json.encodeToString(tabsPaths)
    }

    fun addNewEmptyTab() {
        val newTab = newAppTab()

        _tabsFlow.update {
            it.toMutableList().apply {
                add(newTab)
            }
        }

        _currentTab.value = newTab
    }

    private fun newAppTab(
        tabName: MutableState<String> = mutableStateOf(TabInformation.DEFAULT_NAME),
        path: String? = null,
    ): TabInformation {
        return TabInformation(
            tabName = tabName,
            initialPath = path,
            onTabPathChanged = { updatePersistedTabs() },
            appComponent = appComponent,
        )
    }

    fun onMoveTab(fromIndex: Int, toIndex: Int) {
        _tabsFlow.update {
            it.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
        }

        updatePersistedTabs()
    }
}