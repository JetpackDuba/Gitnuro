package app

import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStateManager @Inject constructor(
    private val appPreferences: AppPreferences,
) {

    private val _openRepositoriesPaths = mutableMapOf<Int, String>()
    val openRepositoriesPathsTabs: Map<Int, String>
        get() = _openRepositoriesPaths

    private val appStateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO) // TODO Stop this when closing the app

    var latestOpenedRepositoryPath: String
        get() = appPreferences.latestOpenedRepositoryPath
        set(value) {
            appPreferences.latestOpenedRepositoryPath = value
        }

    fun repositoryTabChanged(key: Int, path: String) {
        _openRepositoriesPaths[key] = path

        updateSavedRepositoryTabs()
    }

    fun repositoryTabRemoved(key: Int) {
        _openRepositoriesPaths.remove(key)

        updateSavedRepositoryTabs()
    }

    private fun updateSavedRepositoryTabs() = appStateScope.launch(Dispatchers.IO) {
        val tabsList = _openRepositoriesPaths.map { it.value }
        appPreferences.latestTabsOpened = Json.encodeToString(tabsList)
    }

    fun loadRepositoriesTabs() = appStateScope.launch(Dispatchers.IO) {
        val repositoriesSaved = appPreferences.latestTabsOpened

        if (repositoriesSaved.isNotEmpty()) {
            val repositoriesList = Json.decodeFromString<List<String>>(repositoriesSaved)

            repositoriesList.forEachIndexed { index, repository ->
                _openRepositoriesPaths[index] = repository
            }
        }

    }
}