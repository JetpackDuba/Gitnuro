package com.jetpackduba.gitnuro.credentials

import com.jetpackduba.gitnuro.ssh.libssh.LibSshOptions
import com.jetpackduba.gitnuro.ssh.libssh.LibSshSession
import org.apache.sshd.client.SshClient
import org.eclipse.jgit.transport.RemoteSession
import org.eclipse.jgit.transport.URIish
import javax.inject.Inject
import javax.inject.Provider


private const val DEFAULT_SSH_PORT = 22

class GRemoteSession @Inject constructor(
    private val processSession: Provider<LibSshSession>,
    private val credentialsStateManager: CredentialsStateManager,
) : RemoteSession {
    private val client = SshClient.setUpDefaultClient()
    private var session: LibSshSession? = null

    override fun exec(commandName: String, timeout: Int): Process {
        println("Running command $commandName")

        val session = this.session ?: throw Exception("Session is null")
        val process = GProcessLibSsh()

        process.setup(session, commandName)
        return process
    }


    override fun disconnect() {
        client.close()
    }

    fun setup(uri: URIish) {
        val session = processSession.get()
        session.setOptions(LibSshOptions.SSH_OPTIONS_HOST, uri.host)
        session.setOptions(LibSshOptions.SSH_OPTIONS_USER, uri.user)
        session.loadOptionsFromConfig()

        session.connect()
        var result = session.userAuthPublicKeyAuto(null, null)

        if (result == 1) {
            credentialsStateManager.updateState(CredentialsRequested.SshCredentialsRequested)

            var credentials = credentialsStateManager.currentCredentialsState
            while (credentials is CredentialsRequested) {
                credentials = credentialsStateManager.currentCredentialsState
            }

            val password = if (credentials !is CredentialsAccepted.SshCredentialsAccepted)
                null
            else
                credentials.password

            result = session.userAuthPublicKeyAuto(null, password)
        }

        if (result != 0)
            throw Exception("Something went wrong with authentication. Code $result")

        this.session = session
    }
}