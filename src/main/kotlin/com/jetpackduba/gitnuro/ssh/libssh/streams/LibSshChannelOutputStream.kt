package com.jetpackduba.gitnuro.ssh.libssh.streams

import com.jetpackduba.gitnuro.logging.printDebug
import com.jetpackduba.gitnuro.logging.printLog
import com.jetpackduba.gitnuro.ssh.libssh.SSHLibrary
import com.jetpackduba.gitnuro.ssh.libssh.ssh_channel
import java.io.OutputStream

private const val TAG = "LibSshChannelOutputStre"

class LibSshChannelOutputStream(private val sshChannel: ssh_channel) : OutputStream() {
    private val sshLib = SSHLibrary.INSTANCE

    override fun write(b: Int) {
        printLog(TAG, "Write int")

        val byteArrayData = byteArrayOf(b.toByte())
        write(byteArrayData)

        printLog(TAG, "Write int")
    }

    override fun write(b: ByteArray) {
        printLog(TAG, "Write byte")
        sshLib.ssh_channel_write(sshChannel, b, b.size)
        printLog(TAG, "Write byte finished")
    }

    override fun close() {
        printDebug(TAG, "Closing output")
    }
}

fun checkValidResult(result: Int) {
    if (result != 0)
        throw Exception("Result is $result")
}