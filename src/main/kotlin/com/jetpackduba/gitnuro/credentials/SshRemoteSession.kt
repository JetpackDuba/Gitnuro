package com.jetpackduba.gitnuro.credentials

import Session
import com.jetpackduba.gitnuro.exceptions.SshException
import com.jetpackduba.gitnuro.extensions.throwIfSshMessage
import org.eclipse.jgit.transport.CredentialItem
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.RemoteSession
import org.eclipse.jgit.transport.URIish
import javax.inject.Inject


private const val NOT_EXPLICIT_PORT = -1

class SshRemoteSession @Inject constructor() : RemoteSession {
    private lateinit var session: Session
    private lateinit var process: SshProcess
    override fun exec(commandName: String, timeout: Int): Process {
        println("Running command $commandName")

        process = SshProcess()

        process.setup(session, commandName)

        return process
    }

    override fun disconnect() {
        process.closeChannel()
        session.disconnect()
    }

    fun setup(uri: URIish, sshCredentialsProvider: CredentialsProvider) {
        val session = Session.new()
            ?: throw SshException("Could not obtain the session, this is likely a bug. Please file a report.")

        val port = if (uri.port == NOT_EXPLICIT_PORT) {
            null
        } else
            uri.port

        session.setup(uri.host, uri.user ?: "", port).throwIfSshMessage()

        var result = session.publicKeyAuth("")

        if (result == 2) {//AuthStatus.DENIED) {
            val passwordCredentialItem = CredentialItem.Password()
            sshCredentialsProvider.get(uri, passwordCredentialItem)

            val password = passwordCredentialItem.value.joinToString("")

            result = session.publicKeyAuth(password)

            if (result != 1) {//AuthStatus.SUCCESS) {
                result = session.passwordAuth(password)
            }
        }

        if (result != 1) {//AuthStatus.SUCCESS)
            throw Exception("Something went wrong with authentication. Code $result")
        }

        this.session = session
    }
}