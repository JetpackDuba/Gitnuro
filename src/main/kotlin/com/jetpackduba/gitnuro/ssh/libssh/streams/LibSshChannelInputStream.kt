package com.jetpackduba.gitnuro.ssh.libssh.streams

import uniffi.gitnuro.Channel
import java.io.InputStream

class LibSshChannelInputStream(private val sshChannel: Channel) : InputStream() {
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val result = sshChannel.read(false, len.toULong())
        val byteArray = result.data
        val read = result.readCount

        for (i in 0 until len) {
            b[off + i] = byteArray[i]
        }

        return read.toInt()
    }

    override fun read(): Int {

        val result = sshChannel.read(false, 1u)

        val first = result.data.first()

        return first.toInt()
    }

    override fun close() {
        // The channel is closed by [LibSshChannel]
    }
}