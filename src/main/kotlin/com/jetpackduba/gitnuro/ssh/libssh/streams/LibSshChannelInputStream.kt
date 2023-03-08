package com.jetpackduba.gitnuro.ssh.libssh.streams

import com.jetpackduba.gitnuro.ssh.libssh.SSHLibrary
import com.jetpackduba.gitnuro.ssh.libssh.ssh_channel
import java.io.InputStream


class LibSshChannelInputStream(private val sshChannel: ssh_channel) : InputStream() {
    private val sshLib = SSHLibrary.INSTANCE

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val byteArray = ByteArray(len)
        val result = sshLib.ssh_channel_read(sshChannel, byteArray, len, 0)
        for (i in 0 until len) {
            b[off + i] = byteArray[i]
        }

        return result
    }

    override fun read(): Int {
        val buffer = ByteArray(1)

        sshLib.ssh_channel_read(sshChannel, buffer, 1, 0)

        val first = buffer.first()

        print(String(buffer))

        return first.toInt()
    }

    override fun close() {
        println("Closing input")
    }
}