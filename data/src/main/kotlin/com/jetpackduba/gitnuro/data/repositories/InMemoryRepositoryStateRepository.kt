package com.jetpackduba.gitnuro.data.repositories

import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class InMemoryRepositoryStateRepository @Inject constructor() : RepositoryStateRepository {
    override val currentTask: StateFlow<TaskType?>
        field = MutableStateFlow(null)

    override suspend fun <T> runOperation(taskType: TaskType, block: suspend () -> T): T {
        try {
            currentTask.value = taskType
            return block()
        } finally {
            currentTask.value = null
        }
    }
}