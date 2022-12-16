package com.jetpackduba.gitnuro.credentials.streams

class Convertest {
    companion object {
        fun printIt(byteArray: ByteArray) {
            val intList = byteArray.map { it.toInt() }
            println(intList)
        }
    }
}