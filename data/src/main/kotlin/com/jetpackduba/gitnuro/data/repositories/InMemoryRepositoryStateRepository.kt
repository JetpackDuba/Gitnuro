package com.jetpackduba.gitnuro.data.repositories

import com.jetpackduba.gitnuro.domain.MAX_COMPLETED_TASKS_KEPT
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.CompletedTask
import com.jetpackduba.gitnuro.domain.repositories.FailureSeverity
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class InMemoryRepositoryStateRepository @Inject constructor() : RepositoryStateRepository {
    override val currentTask: StateFlow<TaskType?>
        field = MutableStateFlow(null)
    override val completedTasks: StateFlow<List<CompletedTask>>
        field = MutableStateFlow(emptyList())
    override val lastOperationTimestamp: Flow<Long> = completedTasks.map {
        completedTasks.value.lastOrNull()?.date ?: 0L
    }

    override suspend fun <T> runOperation(taskType: TaskType, block: suspend () -> T): T {
        try {
            currentTask.value = taskType
            return block()
        } finally {
            currentTask.value = null
        }
    }

    override suspend fun addCompletedTaskSuccessfully(completedTask: TaskType) {
        addCompletedTask(
            CompletedTask.Success(System.currentTimeMillis(), completedTask)
        )
    }

    override suspend fun addCompletedTaskFailed(
        completedTask: TaskType,
        reason: AppError,
        severity: FailureSeverity
    ) {
        addCompletedTask(
            CompletedTask.Failure(
                System.currentTimeMillis(),
                completedTask,
                reason,
                severity
            )
        )
    }

    private fun addCompletedTask(completedTask: CompletedTask) {
        completedTasks.update { currentTasks ->
            currentTasks
                .toMutableList()
                .apply {
                    this.add(completedTask)
                }
                .takeLast(MAX_COMPLETED_TASKS_KEPT)
        }
    }
}