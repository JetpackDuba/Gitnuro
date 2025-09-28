package com.jetpackduba.gitnuro.utils

import java.io.Closeable

fun <T: Closeable, S: Closeable, R> use(closable1: T, closable2: S, callback: () -> R): R {
    return closable1.use {
        closable2.use {
            callback()
        }
    }
}