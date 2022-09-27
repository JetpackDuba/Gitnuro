package com.jetpackduba.gitnuro.extensions

import java.io.ByteArrayOutputStream
import java.io.InputStream

fun InputStream.toByteArray(): ByteArray {
    val byteArrayOutputStream = ByteArrayOutputStream()
    return byteArrayOutputStream.use { byteArrayOutStream ->
        this.transferTo(byteArrayOutStream)
        byteArrayOutStream.toByteArray()
    }
}