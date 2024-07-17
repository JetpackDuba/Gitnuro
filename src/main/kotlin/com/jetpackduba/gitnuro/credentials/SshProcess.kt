package com.jetpackduba.gitnuro.credentials

import Session
import com.jetpackduba.gitnuro.ssh.libssh.ChannelWrapper
import java.io.InputStream
import java.io.OutputStream

class SshProcess : Process() {
    private lateinit var channel: ChannelWrapper
    private lateinit var session: Session

    private val outputStream by lazy {
        channel.outputStream
    }
    private val inputStream by lazy {
        channel.inputStream
    }
    private val errorOutputStream by lazy {
        channel.errorOutputStream
    }

    override fun getOutputStream(): OutputStream {
        return outputStream
    }

    override fun getInputStream(): InputStream {
        return inputStream
    }

    override fun getErrorStream(): InputStream {
        return errorOutputStream
    }

    override fun waitFor(): Int {
        if (isRunning())
            Thread.sleep(100)

        return exitValue()
    }

    override fun exitValue(): Int {
        check(!isRunning())
        println("exitValue called")

        return 0
    }

    override fun destroy() {
        closeChannel()
    }

    fun closeChannel() {
        channel.close()
    }

    private fun isRunning(): Boolean {
        return channel.isOpen()
    }

    fun setup(session: Session, commandName: String) {
        val channel = ChannelWrapper(session)

        channel.openSession()
        channel.requestExec(commandName)

        this.session = session
        this.channel = channel
    }

}