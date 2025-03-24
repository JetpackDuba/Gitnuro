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

fun InputStream.readUntilValue(limitValue: Int): ByteArray {
    var value: Byte
    val bytesList = mutableListOf<Byte>()

    do {
        value = this.read().toByte()

        if (value.toInt() != limitValue) {
            bytesList.add(value)
        }
    } while (value.toInt() != limitValue)

    return bytesList.toByteArray()
}