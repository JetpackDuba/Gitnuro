package com.jetpackduba.gitnuro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetpackduba.gitnuro.data.repositories.configuration.DataStoreAppSettingsRepository
import com.jetpackduba.gitnuro.di.TabComponent
import com.jetpackduba.gitnuro.domain.models.RepositorySelectionState
import com.jetpackduba.gitnuro.ui.components.TabInformation
import com.jetpackduba.gitnuro.viewmodels.RepositoryTabViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppViewModel @Inject constructor(
    private val appSettingsRepository: DataStoreAppSettingsRepository,
    private val tabComponentFactory: TabComponent.Factory,
) : ViewModel() {
    val tabs: StateFlow<List<TabInformation<RepositoryTabViewModel>>>
        field = MutableStateFlow<List<TabInformation<RepositoryTabViewModel>>>(emptyList())

    val currentTab: StateFlow<TabInformation<RepositoryTabViewModel>?>
        field = MutableStateFlow<TabInformation<RepositoryTabViewModel>?>(null)

    fun loadPersistedTabs() {
        val repositoriesSaved = appSettingsRepository.latestTabsOpened

        val repositoriesList = if (repositoriesSaved.isNotEmpty())
            Json.decodeFromString<List<String>>(repositoriesSaved).map { path ->
                newAppTab2(
                    path = path,
                )
            }
        else
            listOf()

        tabs.value = repositoriesList.ifEmpty { listOf(newAppTab2()) }

        val latestSelectedTabIndex = appSettingsRepository.latestRepositoryTabSelected

        currentTab.value = when (latestSelectedTabIndex < 0) {
            true -> tabs.value.first()
            false -> tabs.value.getOrNull(latestSelectedTabIndex) ?: tabs.value.first()
        }
    }

    fun loadPersistedTabs2() {
        val repositoriesSaved = appSettingsRepository.latestTabsOpened

        val repositoriesList = if (repositoriesSaved.isNotEmpty())
            Json.decodeFromString<List<String>>(repositoriesSaved)
        else
            listOf()

        //
//        val tabs = repositoriesList.map { path ->
//            val tabComponent = tabComponentFactory.create()
//            TabInformation2<RepositoryTabViewModel>()
//        }
    }

    suspend fun addNewTabFromPath(path: String, selectTab: Boolean, tabToBeReplacedPath: String? = null) {
        val tabToBeReplaced = tabs
            .value
            .firstOrNull {
                it.data.repositoryPath.firstOrNull() == tabToBeReplacedPath
            }

        val newTab = newAppTab2(
            path = path,
        )

        tabs.update {
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
            currentTab.value = newTab
        }
    }

    fun selectTab(tab: TabInformation<RepositoryTabViewModel>) {
        currentTab.value = tab

        persistTabSelected(tab)
    }

    private fun persistTabSelected(tab: TabInformation<RepositoryTabViewModel>) {

        appSettingsRepository.latestRepositoryTabSelected = tabs.value.indexOf(tab)
    }

    fun closeTab(tab: TabInformation<RepositoryTabViewModel>) = viewModelScope.launch {
        val tabsList = tabs.value.toMutableList()
        var newCurrentTab: TabInformation<RepositoryTabViewModel>? = null
        tab.data.dispose()

        if (currentTab.value == tab) {
            val index = tabsList.indexOf(tab)

            if (tabsList.count() == 1) {
                newCurrentTab = newAppTab2()
            } else if (index > 0) {
                newCurrentTab = tabsList[index - 1]
            } else if (index == 0) {
                newCurrentTab = tabsList[1]
            }
        }

        tabsList.remove(tab)

        if (newCurrentTab != null) {
            if (!tabsList.contains(newCurrentTab)) {
                tabsList.add(newCurrentTab)
            }

            tabs.value = tabsList
            currentTab.value = newCurrentTab
        } else {
            tabs.value = tabsList
        }

        updatePersistedTabs()
    }

    private suspend fun updatePersistedTabs() {
        val tabs = tabs
            .value
            .filter { it.data.repositorySelectionState.value is RepositorySelectionState.Open }

        val tabsPaths = tabs.map { it.data.repositoryPath.firstOrNull().orEmpty() }

        appSettingsRepository.latestTabsOpened = Json.encodeToString(tabsPaths)
        appSettingsRepository.latestRepositoryTabSelected = tabs.indexOf(currentTab.value)
    }

    fun addNewEmptyTab() {
        val newTab = newAppTab2()

        tabs.update {
            it.toMutableList().apply {
                add(newTab)
            }
        }

        currentTab.value = newTab
    }

    private fun newAppTab2(path: String? = null): TabInformation<RepositoryTabViewModel> {
        val tabComponent: TabComponent = tabComponentFactory.create()
        val viewModel = tabComponent
            .repositoryTabViewModelFactory()
            .create(path)

        return TabInformation(viewModel)
    }

    fun onMoveTab(fromIndex: Int, toIndex: Int) = viewModelScope.launch {
        tabs.update {
            it.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
        }

        updatePersistedTabs()
    }
}
