package com.jetpackduba.gitnuro

import com.jetpackduba.gitnuro.di.qualifiers.AppCoroutineScope
import com.jetpackduba.gitnuro.preferences.AppSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStateManager @Inject constructor(
    private val appSettings: AppSettings,
    @AppCoroutineScope val appScope: CoroutineScope,
) {
    private val mutex = Mutex()

    private val _openRepositoriesPaths = mutableMapOf<Int, String>()
    val openRepositoriesPathsTabs: Map<Int, String>
        get() = _openRepositoriesPaths

    private val _latestOpenedRepositoriesPaths = mutableListOf<String>()
    val latestOpenedRepositoriesPaths: List<String>
        get() = _latestOpenedRepositoriesPaths

    val latestOpenedRepositoryPath: String
        get() = _latestOpenedRepositoriesPaths.firstOrNull() ?: ""

    fun repositoryTabChanged(key: Int, path: String) = appScope.launch(Dispatchers.IO) {
        mutex.lock()
        try {
            // Do not save already saved repos
            if (!_openRepositoriesPaths.containsValue(path))
                _openRepositoriesPaths[key] = path

            // Remove any previously existing path
            _latestOpenedRepositoriesPaths.removeIf { it == path }

            // Add the latest one to the beginning
            _latestOpenedRepositoriesPaths.add(0, path)

            if (_latestOpenedRepositoriesPaths.count() > 10)
                _latestOpenedRepositoriesPaths.removeLast()

            updateSavedRepositoryTabs()
            updateLatestRepositoryTabs()
        } finally {
            mutex.unlock()
        }
    }

    private suspend fun updateSavedRepositoryTabs() = withContext(Dispatchers.IO) {
        val tabsList = _openRepositoriesPaths.map { it.value }
        appSettings.latestTabsOpened = Json.encodeToString(tabsList)
    }

    private suspend fun updateLatestRepositoryTabs() = withContext(Dispatchers.IO) {
        appSettings.latestOpenedRepositoriesPath = Json.encodeToString(_latestOpenedRepositoriesPaths)
    }

    fun loadRepositoriesTabs() {
        val repositoriesPathsSaved = appSettings.latestOpenedRepositoriesPath
        if (repositoriesPathsSaved.isNotEmpty()) {
            val repositories = Json.decodeFromString<List<String>>(repositoriesPathsSaved)
            _latestOpenedRepositoriesPaths.addAll(repositories)
        }
    }

    fun cancelCoroutines() {
        appScope.cancel("Closing com.jetpackduba.gitnuro.app")
    }
}