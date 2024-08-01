package com.jetpackduba.gitnuro.ssh.libssh.streams

import Channel
import com.jetpackduba.gitnuro.exceptions.SshException
import java.io.InputStream

class SshChannelInputErrStream(private val sshChannel: Channel) : InputStream() {
    private var cancelled = false

    override fun read(): Int {
        return if (sshChannel.pollHasBytes(true)) {
            val read = sshChannel.read(true, 1L)
                ?: throw SshException("Could not read result from SSH channel. Please check your network connectivity and try again.")

            val byteArray = read.data

            val first = byteArray.first()

            first.toInt()
        } else
            -1
    }

    override fun close() {
        cancelled = true
    }
}