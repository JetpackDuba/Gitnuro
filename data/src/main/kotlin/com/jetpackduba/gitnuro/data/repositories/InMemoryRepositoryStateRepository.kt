package com.jetpackduba.gitnuro.data.repositories

import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class InMemoryRepositoryStateRepository @Inject constructor() : RepositoryStateRepository {
    override val isProcessing: StateFlow<Boolean>
        field = MutableStateFlow(false)

    override fun isProcessing(value: Boolean) {
        isProcessing.value = value
    }

    override suspend fun <T> runOperation(block: suspend () -> T): T {
        try {
            isProcessing.value = true
            return block()
        } finally {
            isProcessing.value = false
        }
    }
}