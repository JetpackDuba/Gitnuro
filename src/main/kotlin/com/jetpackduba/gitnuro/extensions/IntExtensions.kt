package com.jetpackduba.gitnuro.extensions

fun Int.toStringWithSpaces(charactersCount: Int): String {
    val numberStr = this.toString()
    return if (numberStr.count() == charactersCount)
        numberStr
    else {
        val lengthDiff = charactersCount - numberStr.count()
        val numberBuilder = StringBuilder()
        // Add whitespaces before the numbers
        repeat(lengthDiff) {
            numberBuilder.append(" ")
        }
        numberBuilder.append(numberStr)

        numberBuilder.toString()
    }
}