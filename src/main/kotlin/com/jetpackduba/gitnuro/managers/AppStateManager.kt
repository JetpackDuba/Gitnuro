package com.jetpackduba.gitnuro.managers

import com.jetpackduba.gitnuro.di.qualifiers.AppCoroutineScope
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStateManager @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    @AppCoroutineScope val appScope: CoroutineScope,
) {
    private val mutex = Mutex()

    private val _latestOpenedRepositoriesPaths = MutableStateFlow<List<String>>(emptyList())
    val latestOpenedRepositoriesPaths = _latestOpenedRepositoriesPaths.asStateFlow()

    val latestOpenedRepositoryPath: String
        get() = _latestOpenedRepositoriesPaths.value.firstOrNull() ?: ""

    fun repositoryTabChanged(path: String) = appScope.launch(Dispatchers.IO) {
        mutex.lock()
        try {
            val repoPaths = _latestOpenedRepositoriesPaths.value.toMutableList()

            // Remove any previously existing path
            repoPaths.removeIf { it == path }

            // Add the latest one to the beginning
            repoPaths.add(0, path)

            appSettingsRepository.latestOpenedRepositoriesPath = Json.encodeToString(repoPaths)
            _latestOpenedRepositoriesPaths.value = repoPaths
        } finally {
            mutex.unlock()
        }
    }

    fun loadRepositoriesTabs() {
        val repositoriesPathsSaved = appSettingsRepository.latestOpenedRepositoriesPath
        if (repositoriesPathsSaved.isNotEmpty()) {
            val repositories = Json.decodeFromString<List<String>>(repositoriesPathsSaved)
            val repoPaths = _latestOpenedRepositoriesPaths.value.toMutableList()

            repoPaths.addAll(repositories)

            _latestOpenedRepositoriesPaths.value = repoPaths
        }
    }

    fun cancelCoroutines() {
        appScope.cancel("Closing app")
    }

    fun removeRepositoryFromRecent(path: String) = appScope.launch {
        mutex.lock()
        try {
            val repoPaths = _latestOpenedRepositoriesPaths.value.toMutableList()
            repoPaths.removeIf { it == path }

            appSettingsRepository.latestOpenedRepositoriesPath = Json.encodeToString(repoPaths)
            _latestOpenedRepositoriesPaths.value = repoPaths
        } finally {
            mutex.unlock()
        }
    }
}