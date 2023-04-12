package com.jetpackduba.gitnuro.ssh.libssh.streams

import com.jetpackduba.gitnuro.logging.printLog
import com.jetpackduba.gitnuro.ssh.libssh.SSHLibrary
import com.jetpackduba.gitnuro.ssh.libssh.ssh_channel
import java.io.InputStream

private const val TAG = "LibSshChannelInputErrSt"

class LibSshChannelInputErrStream(private val sshChannel: ssh_channel) : InputStream() {
    private var cancelled = false
    private val sshLib = SSHLibrary.INSTANCE

    override fun read(): Int {
        printLog(TAG, "Read error")
        val buffer = ByteArray(1)

        return if (sshLib.ssh_channel_poll(sshChannel, 1) > 0) {
            sshLib.ssh_channel_read(sshChannel, buffer, 1, 1)

            val first = buffer.first()

            printLog(TAG, "Read error finished ${first.toInt()} - ${String(buffer)}")

            first.toInt()
        } else
            -1
    }

    override fun close() {
        println("Closing error")
        cancelled = true
    }
}