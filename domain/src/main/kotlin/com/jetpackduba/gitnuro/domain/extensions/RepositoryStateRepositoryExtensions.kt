package com.jetpackduba.gitnuro.domain.extensions

import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

inline fun <T> RepositoryStateRepository.runOperationInTabScope(taskType: TaskType, tabScope: TabCoroutineScope, crossinline block: suspend () -> T) {
    tabScope.launch {
        this@runOperationInTabScope.runOperation(taskType) {
            block()
        }
    }
}
inline fun <T> RepositoryStateRepository.runOperationInTabScopeAsync(taskType: TaskType, tabScope: TabCoroutineScope, crossinline block: suspend () -> T): Deferred<T> {
    return tabScope.async {
        this@runOperationInTabScopeAsync.runOperation(taskType) {
            block()
        }
    }
}