package app.extensions

fun <T> List<T>?.countOrZero(): Int {
    return this?.count() ?: 0
}

fun <T> flatListOf(vararg lists: List<T>): List<T> {
    val flatList = mutableListOf<T>()

    for(list in lists) {
        flatList.addAll(list)
    }

    return flatList
}