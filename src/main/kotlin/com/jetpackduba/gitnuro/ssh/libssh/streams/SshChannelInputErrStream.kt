package com.jetpackduba.gitnuro.ssh.libssh.streams

import uniffi.gitnuro.Channel
import java.io.InputStream

class SshChannelInputErrStream(private val sshChannel: Channel) : InputStream() {
    private var cancelled = false

    override fun read(): Int {
        return if (sshChannel.pollHasBytes(true)) {
            val read = sshChannel.read(true, 1u)
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