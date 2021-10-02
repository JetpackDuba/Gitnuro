package credentials

import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.RemoteSession
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.util.FS

class GSessionManager {
    fun generateSshSessionFactory(): SshSessionFactory {
        return object : SshSessionFactory() {
            override fun getSession(
                uri: URIish,
                credentialsProvider: CredentialsProvider?,
                fs: FS?,
                tms: Int
            ): RemoteSession {
                val remoteSession = GRemoteSession()
                remoteSession.setup(uri)
                return remoteSession
            }

            override fun getType(): String {
                return "ssh" //TODO What should be the value of this?
            }

        }
    }
}