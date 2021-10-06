package app.credentials

import org.apache.sshd.client.SshClient
import org.apache.sshd.client.future.ConnectFuture
import org.eclipse.jgit.transport.RemoteSession
import org.eclipse.jgit.transport.URIish

private const val DEFAULT_SSH_PORT = 22

class GRemoteSession : RemoteSession {
    private val client = SshClient.setUpDefaultClient()

    private var connectFuture: ConnectFuture? = null

    override fun exec(commandName: String, timeout: Int): Process {
        println(commandName)
        val connectFuture = checkNotNull(connectFuture)
        val session = connectFuture.clientSession
        session.auth().verify()

        val process = GProcess()
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

        val connectFuture = client.connect(uri.user, uri.host, port)
        connectFuture.await()

        this.connectFuture = connectFuture
    }
}