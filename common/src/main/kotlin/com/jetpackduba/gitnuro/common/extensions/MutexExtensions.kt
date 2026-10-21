package com.jetpackduba.gitnuro.common.extensions


import kotlinx.coroutines.sync.Mutex

suspend inline fun <T> Mutex.lockAndUse(
    block: () -> T,
) : T {
    this.lock()
    return try {
        block()
    } finally {
        this.unlock()
    }
}