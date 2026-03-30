package com.jetpackduba.gitnuro.domain.extensions

fun <T> List<T>?.countOrZero(): Int {
    return this?.count() ?: 0
}

fun <T> flatListOf(vararg lists: List<T>): List<T> {
    val flatList = mutableListOf<T>()

    for (list in lists) {
        flatList.addAll(list)
    }

    return flatList
}

fun <T> List<T>.toMutableAndAdd(item: T): List<T> {
    return this.toMutableList().apply { add(item) }
}

fun <T> List<T>.toMutableAndAddAll(item: List<T>): List<T> {
    return this.toMutableList().apply { addAll(item) }
}

fun <T> Set<T>.toMutableSetAndAdd(item: T): Set<T> {
    return this.toMutableSet().apply { add(item) }
}

fun <T> Set<T>.toMutableSetAndRemove(item: T): Set<T> {
    return this.toMutableSet().apply { remove(item) }
}

fun <T> Set<T>.toMutableSetAndAddAll(item: List<T>): Set<T> {
    return this.toMutableSet().apply { addAll(item) }
}