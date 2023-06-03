package com.jetpackduba.gitnuro.credentials

import com.jetpackduba.gitnuro.ssh.libssh.LibSshChannel
import com.jetpackduba.gitnuro.ssh.libssh.LibSshSession
import java.io.InputStream
import java.io.OutputStream

class GProcessLibSsh : Process() {
    private lateinit var channel: LibSshChannel
    private lateinit var session: LibSshSession

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

        return channel.close()
    }

    override fun destroy() {
        if (channel.isOpen()) {
            channel.close()
        }

        println("Destroy called")
    }

    private fun isRunning(): Boolean {
        return channel.isOpen()
    }

    fun setup(session: LibSshSession, commandName: String) {
        val channel = session.createChannel()

        channel.openSession()
        channel.requestExec(commandName)

        this.session = session
        this.channel = channel
    }

}