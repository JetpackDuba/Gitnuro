package com.jetpackduba.gitnuro.ssh.libssh

import com.jetpackduba.gitnuro.logging.printError
import com.jetpackduba.gitnuro.ssh.libssh.streams.checkValidResult
import javax.inject.Inject

private const val TAG = "LibSshSession"

class LibSshSession @Inject constructor() {
    private val sshLib = SSHLibrary.INSTANCE

    private var session: ssh_session = sshLib.ssh_new()
    private var channel: LibSshChannel? = null

    fun setOptions(option: LibSshOptions, value: String) {
        sshLib.ssh_options_set(session, option.ordinal, value)
    }

    fun loadOptionsFromConfig() {
        checkValidResult("loadOptionsFromConfig", sshLib.ssh_options_parse_config(session, null))
    }

    fun connect() {
        sshLib.ssh_connect(session)
    }

    fun userAuthPublicKeyAuto(username: String?, password: String?): Int {
        val result = sshLib.ssh_userauth_publickey_auto(session, username, password)

        if (result != 0)
            printError(TAG, "Result is: $result.\nError is: ${getError()}")

        return result
    }

    fun userAuthPassword(password: String): Int {
        val result = sshLib.ssh_userauth_password(session, null, password)

        if (result != 0)
            printError(TAG, "Result is: $result.\nError is: ${getError()}")

        return result
    }

    fun createChannel(): LibSshChannel {
        val newChannel = LibSshChannel(session)

        this.channel = newChannel

        return newChannel
    }

    private fun getError(): String {
        return sshLib.ssh_get_error(session)
    }

    fun disconnect() {
        sshLib.ssh_disconnect(session)
    }
}


