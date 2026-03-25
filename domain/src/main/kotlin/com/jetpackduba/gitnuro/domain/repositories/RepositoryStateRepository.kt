package com.jetpackduba.gitnuro.domain.repositories

import kotlinx.coroutines.flow.StateFlow

interface RepositoryStateRepository {
    val isProcessing: StateFlow<Boolean>

    fun isProcessing(value: Boolean)

    suspend fun <T> runOperation(block: suspend () -> T): T
}