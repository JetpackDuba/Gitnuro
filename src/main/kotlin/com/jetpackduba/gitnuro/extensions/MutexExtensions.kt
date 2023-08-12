package com.jetpackduba.gitnuro.extensions

import kotlinx.coroutines.sync.Mutex

suspend fun <T> Mutex.lockUse(block: () -> T): T {
    this.lock()

    try {
        return block()
    } finally {
        this.unlock()
    }
}