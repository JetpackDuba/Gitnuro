package com.jetpackduba.gitnuro.credentials.streams

import com.jetpackduba.gitnuro.credentials.sshLib
import com.jetpackduba.gitnuro.credentials.ssh_channel
import java.io.InputStream


class LibSshInputErrStream(private val sshChannel: ssh_channel) : InputStream() {
    private var cancelled = false
    private var calls = 0

//    override fun read(b: ByteArray, off: Int, len: Int): Int {
//        return sshLib.ssh_channel_read(sshChannel, b, len, 1)
//    }

    override fun read(): Int {
        println("Read error")
        val buffer = ByteArray(1)

        return if (sshLib.ssh_channel_poll(sshChannel, 1) > 0) {
            sshLib.ssh_channel_read(sshChannel, buffer, 1, 1)

            val first = buffer.first()
            println("Read error finished ${first.toInt()}")

            print(String(buffer))

            first.toInt()
        } else
            -1
    }

//    override fun read(b: ByteArray, off: Int, len: Int): Int {
//        calls++
//
//        println("Read error started, call $calls for len of $len with offset $off")
//
//        val byteArray = ByteArray(len)
//        val result = sshLib.ssh_channel_read(sshChannel, byteArray, len, 1)
//        for(i in 0 until len) {
//            b[off + i] = byteArray[i]
//        }
//
//        println("Read ended ${byteArray.map { it.toInt() }}")
//
//        return result
//    }
//
//    override fun read(): Int {
//        val buffer = ByteArray(1)
//
//        sshLib.ssh_channel_read(sshChannel, buffer, 1, 1)
//
//        val first = buffer.first()
//
//        println("Error message is ${String(buffer)}")
//
//        return first.toInt()
//    }

    override fun close() {
        println("Closing error")
        cancelled = true
    }
}