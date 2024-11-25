package com.jetpackduba.gitnuro.extensions

import com.jetpackduba.gitnuro.exceptions.SshException
import com.jetpackduba.gitnuro.system.systemSeparator
import java.math.BigInteger
import java.security.MessageDigest

val String.md5: String
    get() {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(this.toByteArray())).toString(16).padStart(32, '0')
    }

val String.dirName: String
    get() {
        val parts = this.split(systemSeparator)

        return if (parts.isNotEmpty())
            parts.last()
        else
            this
    }

val String.dirPath: String
    get() {
        val parts = this.split(systemSeparator).toMutableList()

        return if (parts.count() > 1) {
            parts.removeLast()
            parts.joinToString(systemSeparator)
        } else
            this
    }

fun String.removeLineDelimiters(): String {
    return this
        .removeSuffix("\r\n")
        .removeSuffix("\n")
}

fun String.replaceTabs(): String {
    return this.replace(
        "\t",
        "    "
    )
}

val String.lineDelimiter: String?
    get() {
        return if (this.contains("\r\n"))
            "\r\n"
        else if (this.contains("\n"))
            "\n"
        else
            null
    }

val String.nullIfEmpty: String?
    get() = this.ifBlank { null }

fun String.lowercaseContains(other: String): Boolean {
    return this.lowercase().contains(other.lowercase().trim())
}

fun String.throwIfSshMessage() {
    if (this.isNotEmpty()) {
        throw SshException(this)
    }
}