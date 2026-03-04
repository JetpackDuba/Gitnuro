package com.jetpackduba.gitnuro.extensions

import androidx.compose.runtime.Composable
import kotlinx.coroutines.sync.Semaphore

suspend inline fun Semaphore.acquireAndUse(
    block: () -> Composable,
) {
    this.acquire()
    try {
        block()
    } finally {
        this.release()
    }
}