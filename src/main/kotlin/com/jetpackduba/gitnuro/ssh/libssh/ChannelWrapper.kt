package com.jetpackduba.gitnuro.ssh.libssh

import com.jetpackduba.gitnuro.ssh.libssh.streams.SshChannelInputErrStream
import com.jetpackduba.gitnuro.ssh.libssh.streams.SshChannelInputStream
import com.jetpackduba.gitnuro.ssh.libssh.streams.SshChannelOutputStream
import uniffi.gitnuro.Channel
import uniffi.gitnuro.Session

class ChannelWrapper internal constructor(sshSession: Session) {
    private var channel = Channel(sshSession)

    val outputStream = SshChannelOutputStream(channel)
    val inputStream = SshChannelInputStream(channel)
    val errorOutputStream = SshChannelInputErrStream(channel)

    fun openSession() {
        channel.openSession()
    }

    fun requestExec(commandName: String) {
        channel.requestExec(commandName)
    }

    fun isOpen(): Boolean {
        return channel.isOpen()
    }

    fun close() {
        channel.closeChannel()
    }
}