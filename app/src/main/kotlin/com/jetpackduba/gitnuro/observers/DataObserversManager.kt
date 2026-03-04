package com.jetpackduba.gitnuro.observers

import com.jetpackduba.gitnuro.common.TabScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import javax.inject.Inject

@TabScope
class DataObserversManager @Inject constructor(
    private val observeBranchRefresh: ObserveBranchRefresh,
) {
    private var observersScope = CoroutineScope(SupervisorJob())

    fun start() {
        observersScope.launch { observeBranchRefresh.startObserving() }
    }

    fun stop() {
        observersScope.coroutineContext.cancelChildren()
    }
}