package com.jetpackduba.gitnuro.ssh.libssh.streams

import uniffi.gitnuro.Channel
import java.io.OutputStream

class LibSshChannelOutputStream(private val sshChannel: Channel) : OutputStream() {
    override fun write(b: Int) {
        val byteArrayData = byteArrayOf(b.toByte())
        write(byteArrayData)
    }

    override fun write(b: ByteArray) {
        sshChannel.writeBytes(b)
    }

    override fun close() {
    }
}
