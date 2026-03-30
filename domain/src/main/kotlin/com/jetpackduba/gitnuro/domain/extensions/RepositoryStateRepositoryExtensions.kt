package com.jetpackduba.gitnuro.domain.extensions

import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

inline fun <T> RepositoryStateRepository.runOperationInTabScope(tabScope: TabCoroutineScope, crossinline block: suspend () -> T) {
    tabScope.launch {
        this@runOperationInTabScope.runOperation {
            block()
        }
    }
}
inline fun <T> RepositoryStateRepository.runOperationInTabScopeAsync(tabScope: TabCoroutineScope, crossinline block: suspend () -> T): Deferred<T> {
    return tabScope.async {
        this@runOperationInTabScopeAsync.runOperation {
            block()
        }
    }
}