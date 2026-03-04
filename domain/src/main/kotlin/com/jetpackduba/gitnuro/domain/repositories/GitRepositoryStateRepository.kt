package com.jetpackduba.gitnuro.domain.repositories

import kotlinx.coroutines.flow.StateFlow

interface GitRepositoryStateRepository {
    val isProcessing: StateFlow<Boolean>
    val repositoryPath: StateFlow<String?>

    suspend fun setPath(path: String)
    suspend fun clearPath(path: String)
}