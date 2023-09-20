package com.jetpackduba.gitnuro.ssh.libssh

import com.jetpackduba.gitnuro.ssh.libssh.streams.LibSshChannelInputErrStream
import com.jetpackduba.gitnuro.ssh.libssh.streams.LibSshChannelInputStream
import com.jetpackduba.gitnuro.ssh.libssh.streams.LibSshChannelOutputStream
import uniffi.gitnuro.Channel
import uniffi.gitnuro.Session

class LibSshChannel internal constructor(sshSession: Session) {
    private var channel = Channel(sshSession)

    val outputStream = LibSshChannelOutputStream(channel)
    val inputStream = LibSshChannelInputStream(channel)
    val errorOutputStream = LibSshChannelInputErrStream(channel)

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
        channel.close()
    }
}