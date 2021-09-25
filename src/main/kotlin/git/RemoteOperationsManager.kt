package git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.CredentialsProvider

class RemoteOperationsManager {
    suspend fun pull(git: Git) = withContext(Dispatchers.IO) {
        git
            .pull()
            .setCredentialsProvider(CredentialsProvider.getDefault())
            .call()
    }

    suspend fun push(git: Git) = withContext(Dispatchers.IO) {
        git
            .push()
            .setPushTags()
            .call()
    }
}