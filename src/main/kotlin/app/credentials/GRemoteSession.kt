package app.credentials

import org.apache.sshd.agent.SshAgent
import org.apache.sshd.agent.local.AgentImpl
import org.apache.sshd.agent.local.LocalAgentFactory
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.auth.keyboard.UserAuthKeyboardInteractive
import org.apache.sshd.client.auth.keyboard.UserAuthKeyboardInteractiveFactory
import org.apache.sshd.client.auth.keyboard.UserInteraction
import org.apache.sshd.client.auth.password.PasswordAuthenticationReporter
import org.apache.sshd.client.auth.password.UserAuthPassword
import org.apache.sshd.client.future.ConnectFuture
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.NamedResource
import org.apache.sshd.common.config.keys.FilePasswordProvider
import org.apache.sshd.common.keyprovider.FileKeyPairProvider
import org.apache.sshd.common.session.SessionContext
import org.eclipse.jgit.transport.RemoteSession
import org.eclipse.jgit.transport.URIish
import java.lang.Exception
import java.security.KeyPair
import java.time.Duration
import javax.inject.Inject
import javax.inject.Provider


private const val DEFAULT_SSH_PORT = 22

class GRemoteSession @Inject constructor(
    private val processProvider: Provider<GProcess>,
) : RemoteSession {
    private val credentialsStateManager = CredentialsStateManager

    private val client = SshClient.setUpDefaultClient()

    private var connectFuture: ConnectFuture? = null

    override fun exec(commandName: String, timeout: Int): Process {
        println(commandName)

        val connectFuture = checkNotNull(connectFuture)

        val session = connectFuture.clientSession

        val auth = session.auth()
        auth.addListener { arg0 ->
            println("Authentication completed with " + if (arg0.isSuccess) "success" else "failure")
        }

        session.waitFor(
            listOf(
                ClientSession.ClientSessionEvent.WAIT_AUTH,
                ClientSession.ClientSessionEvent.CLOSED,
                ClientSession.ClientSessionEvent.AUTHED
            ), Duration.ofHours(2)
        )
        auth.verify()

        val process = processProvider.get()
        process.setup(session, commandName)
        return process
    }


    override fun disconnect() {
        client.close()
    }

    fun setup(uri: URIish) {
        client.open()

        val port = if (uri.port == -1) {
            DEFAULT_SSH_PORT
        } else
            uri.port

        val filePasswordProvider = object : FilePasswordProvider {
            override fun getPassword(session: SessionContext?, resourceKey: NamedResource?, retryIndex: Int): String? {
                credentialsStateManager.updateState(CredentialsState.SshCredentialsRequested)

                var credentials = credentialsStateManager.currentCredentialsState
                while (credentials is CredentialsState.CredentialsRequested) {
                    // TODO check if support for ED25519 with pwd can be added
                    credentials = credentialsStateManager.currentCredentialsState
                }

                return if(credentials !is CredentialsState.SshCredentialsAccepted)
                    null
                else
                    credentials.password
            }
        }

        client.filePasswordProvider = filePasswordProvider

        val connectFuture = client.connect(uri.user, uri.host, port)
        connectFuture.await()

        this.connectFuture = connectFuture
    }
}