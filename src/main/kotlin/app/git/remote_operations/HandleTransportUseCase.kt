package app.git.remote_operations

import app.credentials.GSessionManager
import app.credentials.HttpCredentialsProvider
import org.eclipse.jgit.transport.HttpTransport
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.Transport
import javax.inject.Inject

class HandleTransportUseCase @Inject constructor(
    private val sessionManager: GSessionManager
) {
    operator fun invoke(transport: Transport?) {
        if (transport is SshTransport) {
            transport.sshSessionFactory = sessionManager.generateSshSessionFactory()
        } else if (transport is HttpTransport) {
            transport.credentialsProvider = HttpCredentialsProvider()
        }
    }
}