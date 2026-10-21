package com.jetpackduba.gitnuro.extensions

import kotlinx.coroutines.sync.Semaphore

suspend inline fun <T> Semaphore.acquireAndUse(
    block: () -> T,
): T {
    this.acquire()
    return try {
        block()
    } finally {
        this.release()
    }
}