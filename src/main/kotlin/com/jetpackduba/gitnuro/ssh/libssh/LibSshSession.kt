package com.jetpackduba.gitnuro.ssh.libssh

import javax.inject.Inject

class LibSshSession @Inject constructor() {
    private val sshLib = SSHLibrary.INSTANCE

    private var session: ssh_session = sshLib.ssh_new()
    private var channel: LibSshChannel? = null


    fun setOptions(option: LibSshOptions, value: String) {
        sshLib.ssh_options_set(session, option.ordinal, value)
    }

    fun connect() {
        sshLib.ssh_connect(session)
    }



    fun userAuthPublicKeyAuto(username: String?, password: String?) {
        sshLib.ssh_userauth_publickey_auto(session, username, password)
    }

    fun createChannel(): LibSshChannel {
        val newChannel = LibSshChannel(session)

        this.channel = newChannel

        return newChannel
    }

    fun disconnect() {
        sshLib.ssh_disconnect(session)
    }

}