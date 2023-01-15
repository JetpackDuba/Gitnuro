package com.jetpackduba.gitnuro.ssh.libssh.streams

import com.jetpackduba.gitnuro.ssh.libssh.SSHLibrary
import com.jetpackduba.gitnuro.ssh.libssh.ssh_channel
import java.io.OutputStream

class LibSshChannelOutputStream(private val sshChannel: ssh_channel) : OutputStream() {
    private val sshLib = SSHLibrary.INSTANCE

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

fun checkValidResult(result: Int, callback: (Int) -> Unit) {
    if (result != 0) {
        callback(result)
        throw Exception("Result is $result")
    }
}