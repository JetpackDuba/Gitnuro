package com.jetpackduba.gitnuro.domain.repositories

import com.jetpackduba.gitnuro.domain.models.TaskType
import kotlinx.coroutines.flow.StateFlow

interface RepositoryStateRepository {
    val currentTask: StateFlow<TaskType?>

    suspend fun <T> runOperation(taskType: TaskType, block: suspend () -> T): T
}