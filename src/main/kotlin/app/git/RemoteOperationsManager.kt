package app.git

import app.credentials.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.*

class RemoteOperationsManager {
    private val sessionManager = GSessionManager()

    suspend fun pull(git: Git) = withContext(Dispatchers.IO) {
        git
            .pull()
            .setTransportConfigCallback {
                if (it is SshTransport) {
                    it.sshSessionFactory = sessionManager.generateSshSessionFactory()
                } else if (it is HttpTransport) {
                    it.credentialsProvider = HttpCredentialsProvider()
                }
            }
            .setCredentialsProvider(CredentialsProvider.getDefault())
            .call()
    }

    suspend fun push(git: Git) = withContext(Dispatchers.IO) {
        val currentBranchRefSpec = git.repository.fullBranch

        git
            .push()
            .setRefSpecs(RefSpec(currentBranchRefSpec))
            .setPushTags()
            .setTransportConfigCallback {
                if (it is SshTransport) {
                    it.sshSessionFactory = sessionManager.generateSshSessionFactory()
                } else if (it is HttpTransport) {
                    it.credentialsProvider = HttpCredentialsProvider()
                }
            }
            .call()
    }
}