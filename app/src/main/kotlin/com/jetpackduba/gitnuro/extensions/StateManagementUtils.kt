package com.jetpackduba.gitnuro.extensions

import kotlinx.coroutines.*

/**
 * Calls a code [onDelayTriggered] if [block] has not completed before the time specified in [delayMs].
 * Use case: Sometimes is not worth updating the UI with a state to "loading" if the load code executed afterwards is really
 * fast.
 */
suspend fun <T> delayedStateChange(delayMs: Long, onDelayTriggered: suspend () -> Unit, block: suspend () -> T): T {
    val scope = CoroutineScope(Dispatchers.IO)
    var completed = false

    scope.launch {
        delay(delayMs)
        if (!completed) {
            onDelayTriggered()
        }
    }
    return try {
        val result = block()
        scope.cancel()
        result
    } finally {
        completed = true
    }
}