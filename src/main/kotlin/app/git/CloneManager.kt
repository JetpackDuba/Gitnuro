package app.git

import app.credentials.GSessionManager
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.*
import java.io.File
import java.io.IOException

private const val REMOTE_URL = ""

class CloneManager {
    private val sessionManager = GSessionManager()


    fun cloneTest() {
        // prepare a new folder for the cloned repository

        // prepare a new folder for the cloned repository
        val localPath = File.createTempFile("TestGitRepository", "")
        if (!localPath.delete()) {
            throw IOException("Could not delete temporary file $localPath")
        }

        Git.cloneRepository()
            .setURI(REMOTE_URL)
            .setDirectory(localPath)
            .setTransportConfigCallback {
                if (it is SshTransport) {
                    it.sshSessionFactory = sessionManager.generateSshSessionFactory()
                } else if (it is HttpTransport) {
                    it.credentialsProvider = object : CredentialsProvider() {
                        override fun isInteractive(): Boolean {
                            return true
                        }

                        override fun supports(vararg items: CredentialItem?): Boolean {
                            println(items)

                            return true
                        }

                        override fun get(uri: URIish?, vararg items: CredentialItem?): Boolean {
                            return true
                        }

                    }
                }
            }
            .call().use { result ->
                // Note: the call() returns an opened repository already which needs to be closed to avoid file handle leaks!
                println("Having repository: " + result.repository.directory)
            }
    }
}