package com.jetpackduba.gitnuro.data.git.remotes

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.interfaces.IAddRemoteGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.transport.URIish
import javax.inject.Inject

class AddRemoteGitAction @Inject constructor() : IAddRemoteGitAction {
    override suspend operator fun invoke(
        repositoryPath: String,
        remoteName: String,
        fetchUri: String,
    ) = jgit(repositoryPath) {
        remoteAdd()
            .setName(remoteName)
            .setUri(URIish(fetchUri))
            .call()

        Unit
    }
}