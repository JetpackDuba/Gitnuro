package com.jetpackduba.gitnuro.credentials

import Session
import org.eclipse.jgit.transport.RemoteSession
import org.eclipse.jgit.transport.URIish
import java.util.concurrent.CancellationException
import javax.inject.Inject


private const val NOT_EXPLICIT_PORT = -1

class SshRemoteSession @Inject constructor(
    private val credentialsStateManager: CredentialsStateManager,
) : RemoteSession {
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

    fun setup(uri: URIish) {
        val session = Session.new()

        val port = if (uri.port == NOT_EXPLICIT_PORT) {
            null
        } else
            uri.port

        session.setup(uri.host, uri.user ?: "", port)//?.toUShort())

        var result = session.publicKeyAuth("")

        if (result == 2) {//AuthStatus.DENIED) {
            credentialsStateManager.updateState(CredentialsRequested.SshCredentialsRequested)

            var credentials = credentialsStateManager.currentCredentialsState
            while (credentials is CredentialsRequested) {
                credentials = credentialsStateManager.currentCredentialsState
            }

            val password = if (credentials !is CredentialsAccepted.SshCredentialsAccepted)
                throw CancellationException("Credentials cancelled")
            else
                credentials.password

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