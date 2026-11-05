package com.jetpackduba.gitnuro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetpackduba.gitnuro.di.TabComponent
import com.jetpackduba.gitnuro.domain.models.RepositorySelectionState
import com.jetpackduba.gitnuro.domain.repositories.GlobalSettingsRepository
import com.jetpackduba.gitnuro.domain.repositories.TabsOpenRepository
import com.jetpackduba.gitnuro.domain.usecases.CleanRepositoriesResourcesUseCase
import com.jetpackduba.gitnuro.ui.components.TabInformation
import com.jetpackduba.gitnuro.viewmodels.RepositoryTabViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppViewModel @Inject constructor(
    private val tabsOpenRepository: TabsOpenRepository,
    private val tabComponentFactory: TabComponent.Factory,
    private val cleanRepositoriesResourcesUseCase: CleanRepositoriesResourcesUseCase,
) : ViewModel() {
    val tabs: StateFlow<List<TabInformation<RepositoryTabViewModel>>>
        field = MutableStateFlow<List<TabInformation<RepositoryTabViewModel>>>(emptyList())

    val currentTab: StateFlow<TabInformation<RepositoryTabViewModel>?>
        field = MutableStateFlow<TabInformation<RepositoryTabViewModel>?>(null)

    val groups: StateFlow<Map<String, List<String>>>
        field = MutableStateFlow<Map<String, List<String>>>(emptyMap())

    val activeGroup: StateFlow<String>
        field = MutableStateFlow("")

    suspend fun loadPersistedTabs() {
        activeGroup.value = tabsOpenRepository.activeGroup.first()
        groups.value = tabsOpenRepository.groups.first().let {
            if (activeGroup.value in it) it else mapOf(activeGroup.value to emptyList<String>()) + it
        }

        val repositoriesSaved = tabsOpenRepository.openTabs.firstOrNull().orEmpty()
        val repositoriesList = repositoriesSaved.map { path ->
            newAppTab2(
                path = path,
            )
        }

        tabs.value = repositoriesList.ifEmpty { listOf(newAppTab2()) }

        val latestSelectedTabIndex = tabsOpenRepository.selectedTabIndex.firstOrNull() ?: -1

        currentTab.value = when (latestSelectedTabIndex < 0) {
            true -> tabs.value.first()
            false -> tabs.value.getOrNull(latestSelectedTabIndex) ?: tabs.value.first()
        }
    }

    suspend fun addNewTabFromPath(path: String, selectTab: Boolean, tabToBeReplacedPath: RepositoryTabViewModel? = null) {
        val tabToBeReplaced = tabs
            .value
            .firstOrNull {
                it.data == tabToBeReplacedPath
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

        updatePersistedTabs()
    }

    fun selectTab(tab: TabInformation<RepositoryTabViewModel>) {
        currentTab.value = tab

        viewModelScope.launch {
            updatePersistedTabs()
        }
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

        val remainingTabs = tabsList.mapNotNull {
            if (it.data.isLoaded) {
                it.data.repositoryPath.value
            } else {
                null
            }
        }

        cleanRepositoriesResourcesUseCase(remainingTabs)

        updatePersistedTabs()
        System.gc()
    }

    fun switchGroup(name: String) = viewModelScope.launch {
        if (name == activeGroup.value || name !in groups.value) return@launch

        updatePersistedTabs()
        groups.update { it + (activeGroup.value to tabsOpenRepository.openTabs.first()) }

        val paths = groups.value.getValue(name)
        val newTabs = paths.map { newAppTab2(it) }.ifEmpty { listOf(newAppTab2()) }

        tabs.value.forEach { it.data.dispose() }
        tabs.value = newTabs
        currentTab.value = newTabs.first()
        activeGroup.value = name

        cleanRepositoriesResourcesUseCase(paths)
        tabsOpenRepository.updateGroups(groups.value)
        tabsOpenRepository.updateActiveGroup(name)
        tabsOpenRepository.updateOpenTabs(paths)
        tabsOpenRepository.updateSelectedTabIndex(0)
        System.gc()
    }

    fun createGroup(name: String) {
        if (name in groups.value) return

        groups.update { it + (name to emptyList<String>()) }
        switchGroup(name)
    }

    fun renameGroup(oldName: String, newName: String) = viewModelScope.launch {
        if (newName in groups.value) return@launch

        groups.update { it.mapKeys { (key, _) -> if (key == oldName) newName else key } }
        if (activeGroup.value == oldName) {
            activeGroup.value = newName
            tabsOpenRepository.updateActiveGroup(newName)
        }
        tabsOpenRepository.updateGroups(groups.value)
    }

    fun deleteGroup(name: String) = viewModelScope.launch {
        if (name == activeGroup.value) return@launch

        groups.update { it - name }
        tabsOpenRepository.updateGroups(groups.value)
    }

    suspend fun updatePersistedTabs() {
        val tabsToPersist = tabs
            .value
            .mapNotNull {
                when (val selectionState = it.data.repositorySelectionState.value) {
                    RepositorySelectionState.None -> null
                    is RepositorySelectionState.Open -> it to selectionState.path
                    is RepositorySelectionState.Opening -> it to selectionState.path
                    RepositorySelectionState.Unknown -> {
                        val path = it.data.repositoryPath.value ?: return@mapNotNull null
                        it to path
                    }
                }
            }

        val paths = tabsToPersist.map { it.second }
        tabsOpenRepository.updateOpenTabs(paths)
        tabsOpenRepository.updateSelectedTabIndex(tabsToPersist.indexOfFirst { it.first == currentTab.value })
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
