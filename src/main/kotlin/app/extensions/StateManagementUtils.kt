package app.extensions

import kotlinx.coroutines.*

/**
 * Calls a code [onDelayTriggered] if [block] has not completed before the time specified in [delayMs].
 * Use case: Sometimes is not worth updating the UI with a state to "loading" if the load code executed afterwards is really
 * fast.
 */
suspend fun delayedStateChange(delayMs: Long, onDelayTriggered: suspend () -> Unit, block: suspend () -> Unit) {
    val scope = CoroutineScope(Dispatchers.IO)
    var completed = false

    scope.launch {
        delay(delayMs)
        if (!completed) {
            onDelayTriggered()
        }
    }
    try {
        block()
        scope.cancel()
    } finally {
        completed = true
    }
}