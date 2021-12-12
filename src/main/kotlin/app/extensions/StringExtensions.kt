package app.extensions

import java.math.BigInteger
import java.security.MessageDigest

val String.md5: String
    get() {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(this.toByteArray())).toString(16).padStart(32, '0')
    }

val String.dirName: String
    get() {
        val parts = this.split("/")

        return if (parts.isNotEmpty())
            parts.last()
        else
            this
    }

val String.dirPath: String
    get() {
        val parts = this.split("/").toMutableList()

        return if (parts.count() > 1) {
            parts.removeLast()
            parts.joinToString("/")
        } else
            this
    }