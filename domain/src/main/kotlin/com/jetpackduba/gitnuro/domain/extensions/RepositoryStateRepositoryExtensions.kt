package com.jetpackduba.gitnuro.domain.extensions

import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

inline fun <T> RepositoryStateRepository.runOperationInTabScope(tabScope: CoroutineScope, crossinline block: suspend () -> T) {
    tabScope.launch {
        this@runOperationInTabScope.runOperation {
            block()
        }
    }
}