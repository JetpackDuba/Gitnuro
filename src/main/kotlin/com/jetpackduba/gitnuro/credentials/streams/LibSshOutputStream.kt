package com.jetpackduba.gitnuro.credentials.streams

import com.jetpackduba.gitnuro.credentials.sshLib
import com.jetpackduba.gitnuro.credentials.ssh_channel
import java.io.OutputStream
import java.nio.ByteBuffer

class LibSshOutputStream(private val sshChannel: ssh_channel) : OutputStream() {
    override fun write(b: Int) {
        println("write int")

        val byteArrayData = byteArrayOf(b.toByte())
        write(byteArrayData)

        println("write int finished")
    }

    override fun write(b: ByteArray) {
        println("write byte")
        sshLib.ssh_channel_write(sshChannel, b, b.size)
        println("write byte finished")
    }

    override fun close() {
        println("Closing output")
    }
}


fun checkValidResult(result: Int) {
    if (result != 0)
        throw Exception("Result is $result")
}