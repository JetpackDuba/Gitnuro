package com.jetpackduba.gitnuro.git.remotes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.URIish
import javax.inject.Inject

class AddRemoteUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, remoteName: String, fetchUri: String): Unit = withContext(Dispatchers.IO) {
        git.remoteAdd()
            .setName(remoteName)
            .setUri(URIish(fetchUri))
            .call()
    }
}