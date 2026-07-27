package com.jetpackduba.gitnuro.domain

import com.jetpackduba.gitnuro.domain.repositories.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

// TODO Split this class into individual use cases
@Singleton
class AppStateManager @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
) {
    private val mutex = Mutex()

    private val _latestOpenedRepositoriesPaths = MutableStateFlow<List<String>>(emptyList())
    val latestOpenedRepositoriesPaths = _latestOpenedRepositoriesPaths.asStateFlow()

    val latestOpenedRepositoryPath: String
        get() = _latestOpenedRepositoriesPaths.value.firstOrNull() ?: ""

    suspend fun repositoryTabChanged(path: String) = withContext(Dispatchers.IO) {
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

    suspend fun removeRepositoryFromRecent(path: String) {
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