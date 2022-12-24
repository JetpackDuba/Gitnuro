package com.jetpackduba.gitnuro.ssh.libssh

import com.jetpackduba.gitnuro.ssh.libssh.streams.LibSshChannelInputErrStream
import com.jetpackduba.gitnuro.ssh.libssh.streams.LibSshChannelInputStream
import com.jetpackduba.gitnuro.ssh.libssh.streams.LibSshChannelOutputStream

class LibSshChannel internal constructor(sshSession: ssh_session) {
    private val sshLib = SSHLibrary.INSTANCE
    private var channel: ssh_channel = sshLib.ssh_channel_new(sshSession)

    val outputStream = LibSshChannelOutputStream(channel)
    val inputStream = LibSshChannelInputStream(channel)
    val errorOutputStream = LibSshChannelInputErrStream(channel)


    fun openSession() {
        sshLib.ssh_channel_open_session(channel)
    }

    fun requestExec(commandName: String) {
        sshLib.ssh_channel_request_exec(channel, commandName)
    }

    fun isOpen(): Boolean {
        return sshLib.ssh_channel_is_open(channel) == 1
    }

    fun close(): Int {
        return sshLib.ssh_channel_close(channel)
    }

}