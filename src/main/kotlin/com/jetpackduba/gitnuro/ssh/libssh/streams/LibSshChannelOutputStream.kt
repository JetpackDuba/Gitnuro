package com.jetpackduba.gitnuro.ssh.libssh.streams

import com.jetpackduba.gitnuro.ssh.libssh.SSHLibrary
import com.jetpackduba.gitnuro.ssh.libssh.ssh_channel
import java.io.OutputStream

class LibSshChannelOutputStream(private val sshChannel: ssh_channel) : OutputStream() {
    private val sshLib = SSHLibrary.INSTANCE

    override fun write(b: Int) {
        val byteArrayData = byteArrayOf(b.toByte())
        write(byteArrayData)
    }

    override fun write(b: ByteArray) {
        sshLib.ssh_channel_write(sshChannel, b, b.size)
    }

    override fun close() {
    }
}

fun checkValidResult(result: Int) {
    if (result != 0)
        throw Exception("Result is $result")
}