package com.jetpackduba.gitnuro.credentials

import com.jetpackduba.gitnuro.credentials.streams.LibSshInputErrStream
import com.jetpackduba.gitnuro.credentials.streams.LibSshInputStream
import com.jetpackduba.gitnuro.credentials.streams.LibSshOutputStream
import com.jetpackduba.gitnuro.credentials.streams.checkValidResult
import java.io.InputStream
import java.io.OutputStream

class GProcessLibSsh : Process() {
    private lateinit var channel: ssh_channel
    private lateinit var session: ssh_session

    private val outputStream by lazy {
        LibSshOutputStream(channel)
    }
    private val inputStream by lazy {
        LibSshInputStream(channel)
    }
    private val errorOutputStream by lazy {
        LibSshInputErrStream(channel)
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

        return sshLib.ssh_channel_close(channel)
    }

    override fun destroy() {
        if (sshLib.ssh_channel_is_open(channel) == 1) {
            checkValidResult(sshLib.ssh_channel_close(channel))
        }

        sshLib.ssh_disconnect(session)
        println("Destroy called")
    }

    private fun isRunning(): Boolean {
        return sshLib.ssh_channel_is_open(channel) == 1
    }

    fun setup(session: ssh_session, commandName: String) {
        val channel = sshLib.ssh_channel_new(session)

        checkValidResult(sshLib.ssh_channel_open_session(channel))
        checkValidResult(sshLib.ssh_channel_request_exec(channel, commandName))

        this.session = session
        this.channel = channel
    }

}