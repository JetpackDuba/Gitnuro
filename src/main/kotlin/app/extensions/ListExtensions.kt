package app.extensions

fun <T> List<T>?.countOrZero(): Int {
    return this?.count() ?: 0
}