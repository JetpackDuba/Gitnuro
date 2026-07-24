package com.jetpackduba.gitnuro.domain.repositories

import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.usecases.DataToRefresh
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface RepositoryStateRepository {
    val currentTask: StateFlow<TaskType?>
    val completedTasks: StateFlow<List<CompletedTask>>
    val lastOperationTimestamp: Flow<Long>
    val refreshTriggered: StateFlow<List<DataToRefresh>>

    suspend fun <T> runOperation(taskType: TaskType, block: suspend () -> T): T
    suspend fun addCompletedTaskSuccessfully(completedTask: TaskType)
    suspend fun addCompletedTaskFailed(completedTask: TaskType, reason: AppError, severity: FailureSeverity)
    suspend fun refreshTriggered(dataToRefresh: List<DataToRefresh>)
}

sealed interface CompletedTask {
    val date: Long
    val taskType: TaskType

    data class Success(
        override val date: Long,
        override val taskType: TaskType
    ) : CompletedTask

    data class Failure(
        override val date: Long,
        override val taskType: TaskType,
        val reason: AppError,
        val severity: FailureSeverity,
    ) : CompletedTask
}

enum class FailureSeverity {
    LOW,
    HIGH,
}


/*
completedTasks -> Ok or Error

Ok = only the task type
Err = Task type + severity

UI collects both to show notifications
Specific components may observe them to detect potentially related events to the currently displayed UI. An example
is updating the diff windows when a line was staged for a specific diff (or even the whole file).



 */