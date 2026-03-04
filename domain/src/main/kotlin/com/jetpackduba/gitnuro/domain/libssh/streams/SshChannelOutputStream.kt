package com.jetpackduba.gitnuro.domain.libssh.streams

import Channel
import com.jetpackduba.gitnuro.domain.extensions.throwIfSshMessage
import java.io.OutputStream

class SshChannelOutputStream(private val sshChannel: Channel) : OutputStream() {
    override fun write(b: Int) {
        val byteArrayData = byteArrayOf(b.toByte())
        write(byteArrayData)
    }

    override fun write(b: ByteArray) {
        sshChannel.writeBytes(b).throwIfSshMessage()
    }

    override fun close() {
    }
}
