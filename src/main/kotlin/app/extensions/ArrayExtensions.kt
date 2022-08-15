package app.extensions

fun <T> Array<T>.matchingIndexes(filter: (T) -> Boolean): List<Int> {
    val matchingIndexes = mutableListOf<Int>()

    this.forEachIndexed { index, item ->
        if (filter(item)) {
            matchingIndexes.add(index)
        }
    }

    return matchingIndexes
}