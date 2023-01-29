package com.jetpackduba.gitnuro.ssh.libssh

import com.jetpackduba.gitnuro.extensions.getCurrentOs
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.PointerType

class ssh_session : PointerType()
class ssh_channel : PointerType()

@Suppress("FunctionName")
interface SSHLibrary : Library {
    fun ssh_new(): ssh_session
    fun ssh_disconnect(session: ssh_session): ssh_session
    fun ssh_options_set(session: ssh_session, enumValue: Int, value: String)

    fun ssh_options_parse_config(session: ssh_session, fileName: String?): Int

    fun ssh_connect(session: ssh_session) : Int

    fun ssh_userauth_agent(session: ssh_session, username: String?): Int
    fun ssh_userauth_publickey_auto(session: ssh_session, username: String?, password: String?): Int
    fun ssh_get_error(session: ssh_session): String

    fun ssh_channel_new(sshSession: ssh_session): ssh_channel

    fun ssh_channel_open_session(sshChannel: ssh_channel): Int

    fun ssh_channel_request_exec(sshChannel: ssh_channel, command: String): Int

    fun ssh_channel_read(sshChannel: ssh_channel, buffer: ByteArray, count: Int, isStderr: Int): Int
    fun ssh_channel_read_timeout(sshChannel: ssh_channel, buffer: ByteArray, count: Int, isStderr: Int, timeoutMs: Int): Int
    fun ssh_channel_poll(sshChannel: ssh_channel, isStderr: Int): Int
    fun ssh_channel_read_nonblocking(sshChannel: ssh_channel, buffer: ByteArray, count: Int, isStderr: Int): Int
    fun ssh_channel_write(sshChannel: ssh_channel, data: ByteArray, len: Int): Int

    fun ssh_channel_close(sshChannel: ssh_channel): Int

    fun ssh_channel_send_eof(sshChannel: ssh_channel): Int

    fun ssh_channel_free(sshChannel: ssh_channel)
    fun ssh_channel_is_open(sshChannel: ssh_channel): Int


    companion object {
        val INSTANCE = Native.loadLibrary("ssh", SSHLibrary::class.java) as SSHLibrary
    }
}