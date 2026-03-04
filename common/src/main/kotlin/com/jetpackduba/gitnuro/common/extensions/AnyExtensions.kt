package com.jetpackduba.gitnuro.common.extensions

inline fun <T> T.nullIf(predicate: (T) -> Boolean): T? {
    return if (predicate(this)) {
        null
    } else {
        this
    }
}

inline fun <T> T.runIf(condition: Boolean, predicate: T.() -> T): T {
    return if (condition) {
        predicate()
    } else {
        this
    }
}