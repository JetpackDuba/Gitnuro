package app.credentials

import org.apache.sshd.client.channel.ChannelExec
import org.apache.sshd.client.session.ClientSession
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

class GProcess : Process() {
    private lateinit var channel: ChannelExec
    private val outputStream = PipedOutputStream()
    private val inputStream = PipedInputStream()
    private val errorOutputStream = PipedOutputStream()
    private val pipedInputStream = PipedInputStream(outputStream)
    private val pipedOutputStream = PipedOutputStream(inputStream)
    private val pipedErrorInputStream = PipedInputStream(errorOutputStream)

    override fun getOutputStream(): OutputStream {
        return pipedOutputStream
    }

    override fun getInputStream(): InputStream {
        return pipedInputStream
    }

    override fun getErrorStream(): InputStream {
        return pipedErrorInputStream
    }

    override fun waitFor(): Int {
        if (isRunning())
            Thread.sleep(100)

        return exitValue()
    }

    override fun exitValue(): Int {
        check(!isRunning())

        return channel.exitStatus
    }

    override fun destroy() {
        if (channel.isOpen) {
            channel.close()
        }
    }

    private fun isRunning(): Boolean {
        return channel.exitStatus < 0 && channel.isOpen
    }

    fun setup(session: ClientSession, commandName: String) {
        val channel = session.createExecChannel(commandName)
        channel.out = outputStream
        channel.err = errorOutputStream
        channel.`in` = inputStream

        channel.open().verify()

        this.channel = channel
    }

}