package com.jetpackduba.gitnuro.credentials

import org.apache.sshd.client.SshClient
import org.apache.sshd.client.future.ConnectFuture
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.config.keys.FilePasswordProvider
import org.eclipse.jgit.transport.RemoteSession
import org.eclipse.jgit.transport.URIish
import java.time.Duration
import javax.inject.Inject
import javax.inject.Provider


private const val DEFAULT_SSH_PORT = 22

class GRemoteSession @Inject constructor(
    private val processProvider: Provider<GProcess>,
    private val credentialsStateManager: CredentialsStateManager,
) : RemoteSession {
    private val client = SshClient.setUpDefaultClient()

    private var connectFuture: ConnectFuture? = null
    private var ssh_session: ssh_session? = null

    override fun exec(commandName: String, timeout: Int): Process {
        println(commandName)
//        val session = connectFuture!!.clientSession
//
//        val auth = session.auth()
//        auth.addListener { arg0 ->
//            println("Authentication completed with " + if (arg0.isSuccess) "success" else "failure")
//        }
//
//        session.waitFor(
//            listOf(
//                ClientSession.ClientSessionEvent.WAIT_AUTH,
//                ClientSession.ClientSessionEvent.CLOSED,
//                ClientSession.ClientSessionEvent.AUTHED
//            ), Duration.ofHours(2)
//        )
//        auth.verify()
//        val process = processProvider.get()

        val session = this.ssh_session ?: throw Exception("Session is null")
        val process = GProcessLibSsh()

        process.setup(session, commandName)
        return process
    }


    override fun disconnect() {
        client.close()
    }

    fun setup(uri: URIish) {

        val session = sshLib.ssh_new()
        sshLib.ssh_options_set(session, 0, uri.host)
        sshLib.ssh_connect(session)
        checkValidResult(sshLib.ssh_userauth_publickey_auto(session, uri.user, null))

        this.ssh_session = session

//        client.open()
//
//        val port = if (uri.port == -1) {
//            DEFAULT_SSH_PORT
//        } else
//            uri.port
//
//        val filePasswordProvider =
//            FilePasswordProvider { _, _, _ ->
//                credentialsStateManager.updateState(CredentialsState.SshCredentialsRequested)
//
//                var credentials = credentialsStateManager.currentCredentialsState
//                while (credentials is CredentialsState.CredentialsRequested) {
//                    credentials = credentialsStateManager.currentCredentialsState
//                }
//
//                if (credentials !is CredentialsState.SshCredentialsAccepted)
//                    null
//                else
//                    credentials.password
//            }
//
//        client.filePasswordProvider = filePasswordProvider
//
//        val connectFuture = client.connect(uri.user, uri.host, port)
//        connectFuture.await()
//
//        this.connectFuture = connectFuture
    }
}

fun checkValidResult(result: Int) {
    if (result != 0)
        throw Exception("Result is $result")
}