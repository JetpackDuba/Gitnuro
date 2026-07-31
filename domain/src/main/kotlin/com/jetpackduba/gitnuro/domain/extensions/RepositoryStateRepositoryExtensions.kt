package com.jetpackduba.gitnuro.domain.extensions

import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

inline fun <T> RepositoryStateRepository.runOperationInTabScope(
    taskType: TaskType,
    tabScope: CoroutineScope,
    isForegroundTask: Boolean,
    crossinline block: suspend () -> T
): Job {
    return tabScope.launch {
        this@runOperationInTabScope.runOperation(taskType, isForegroundTask) {
            block()
        }
    }
}
inline fun <T> RepositoryStateRepository.runOperationInTabScopeAsync(
    taskType: TaskType,
    tabScope: CoroutineScope,
    isForegroundTask: Boolean,
    crossinline block: suspend () -> T
): Deferred<T> {
    return tabScope.async {
        this@runOperationInTabScopeAsync.runOperation(taskType, isForegroundTask) {
            block()
        }
    }
}