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

    private val _latestOpenedRepositoriesPaths = mutableListOf<String>()
    val latestOpenedRepositoriesPaths: List<String>
        get() = _latestOpenedRepositoriesPaths

    private val appStateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO) // TODO Stop this when closing the app

    val latestOpenedRepositoryPath: String
        get() = _latestOpenedRepositoriesPaths.firstOrNull() ?: ""

    fun repositoryTabChanged(key: Int, path: String) = appStateScope.launch(Dispatchers.IO) {
        // Do not save already saved repos
        if (!_openRepositoriesPaths.containsValue(path))
            _openRepositoriesPaths[key] = path

        // Remove any previously existing path
        _latestOpenedRepositoriesPaths.remove(path)

        // Add the latest one to the beginning
        _latestOpenedRepositoriesPaths.add(0, path)

        if (_latestOpenedRepositoriesPaths.count() > 5)
            _latestOpenedRepositoriesPaths.removeLast()

        updateSavedRepositoryTabs()
        updateLatestRepositoryTabs()
    }

    fun repositoryTabRemoved(key: Int) = appStateScope.launch(Dispatchers.IO) {
        _openRepositoriesPaths.remove(key)

        updateSavedRepositoryTabs()
    }

    private suspend fun updateSavedRepositoryTabs() = withContext(Dispatchers.IO) {
        val tabsList = _openRepositoriesPaths.map { it.value }
        appPreferences.latestTabsOpened = Json.encodeToString(tabsList)
    }

    private suspend fun updateLatestRepositoryTabs() = withContext(Dispatchers.IO) {
        appPreferences.latestOpenedRepositoriesPath = Json.encodeToString(_latestOpenedRepositoriesPaths)
    }

    fun loadRepositoriesTabs() = appStateScope.launch(Dispatchers.IO) {
        val repositoriesSaved = appPreferences.latestTabsOpened

        if (repositoriesSaved.isNotEmpty()) {
            val repositoriesList = Json.decodeFromString<List<String>>(repositoriesSaved)

            repositoriesList.forEachIndexed { index, repository ->
                _openRepositoriesPaths[index] = repository
            }
        }

        val repositoriesPathsSaved = appPreferences.latestOpenedRepositoriesPath
        if(repositoriesPathsSaved.isNotEmpty()) {
            val repositories = Json.decodeFromString<List<String>>(repositoriesPathsSaved)
            _latestOpenedRepositoriesPaths.addAll(repositories)
        }
    }
}