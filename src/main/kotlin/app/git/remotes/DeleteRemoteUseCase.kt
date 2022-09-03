package app.git.remotes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class DeleteRemoteUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, remoteName: String): Unit = withContext(Dispatchers.IO) {
        git.remoteRemove()
            .setRemoteName(remoteName)
            .call()
    }
}