package com.jetpackduba.gitnuro.ssh.libssh.streams

import Channel
import com.jetpackduba.gitnuro.exceptions.SshException
import java.io.InputStream

class SshChannelInputStream(private val sshChannel: Channel) : InputStream() {
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val result = sshChannel.read(false, len.toLong())
            ?: throw SshException("Could not read result from SSH channel. Please check your network connectivity and try again.")

        if (result.readCount == 0L) {
            return -1
        }

        val byteArray = result.data
        val read = result.readCount

        for (i in 0 until len) {
            b[off + i] = byteArray[i]
        }

        return read.toInt()
    }

    override fun read(): Int {

        val result = sshChannel.read(false, 1L)
            ?: throw SshException("Could not read result from SSH channel. Please check your network connectivity and try again.")

        if (result.readCount == 0L) {
            return -1
        }

        val first = result.data.first()

        return first.toInt()
    }

    override fun close() {
        // The channel is closed by [LibSshChannel]
    }
}