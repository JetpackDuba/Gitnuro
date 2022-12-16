package com.jetpackduba.gitnuro.credentials.streams

import com.jetpackduba.gitnuro.credentials.sshLib
import com.jetpackduba.gitnuro.credentials.ssh_channel
import java.io.InputStream


class LibSshInputStream(private val sshChannel: ssh_channel) : InputStream() {
    private var calls = 0

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        calls++

        println("Read started, call $calls for len of $len with offset $off")

        val byteArray = ByteArray(len)
        val result = sshLib.ssh_channel_read(sshChannel, byteArray, len, 0)
        for(i in 0 until len) {
            b[off + i] = byteArray[i]
        }

        println("Read ended ${byteArray.map { it.toInt() }}")

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