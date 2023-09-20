package com.jetpackduba.gitnuro.credentials

import org.eclipse.jgit.transport.RemoteSession
import org.eclipse.jgit.transport.URIish
import uniffi.gitnuro.Session
import javax.inject.Inject


private const val DEFAULT_SSH_PORT = 22

class GRemoteSession @Inject constructor(
    private val credentialsStateManager: CredentialsStateManager,
) : RemoteSession {
    private var session: Session? = null

    override fun exec(commandName: String, timeout: Int): Process {
        println("Running command $commandName")

        val session = this.session ?: throw Exception("Session is null")
        val process = GProcessLibSsh()

        process.setup(session, commandName)
        return process
    }

    override fun disconnect() {
        session?.disconnect()
    }

    fun setup(uri: URIish) {
        val session = Session()
        session.setup(uri.host, uri.user, uri.port)

        var result = session.publicKeyAuth()

//        if (result == 1) {
//            credentialsStateManager.updateState(CredentialsRequested.SshCredentialsRequested)
//
//            var credentials = credentialsStateManager.currentCredentialsState
//            while (credentials is CredentialsRequested) {
//                credentials = credentialsStateManager.currentCredentialsState
//            }
//
//            val password = if (credentials !is CredentialsAccepted.SshCredentialsAccepted)
//                throw CancellationException("Credentials cancelled")
//            else
//                credentials.password
//
//            result = session.userAuthPublicKeyAuto(null, password)
//
//            if (result != 0) {
//                result = session.userAuthPassword(password)
//            }
//        }
//
//        if (result != 0)
//            throw Exception("Something went wrong with authentication. Code $result")

        this.session = session
    }
}