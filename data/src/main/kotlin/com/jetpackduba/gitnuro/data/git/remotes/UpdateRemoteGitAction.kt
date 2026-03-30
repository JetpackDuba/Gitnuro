package com.jetpackduba.gitnuro.data.git.remotes

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.interfaces.IUpdateRemoteGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.RemoteSetUrlCommand
import org.eclipse.jgit.transport.URIish
import javax.inject.Inject

class UpdateRemoteGitAction @Inject constructor() : IUpdateRemoteGitAction {
    override suspend operator fun invoke(
        repositoryPath: String,
        remoteName: String,
        uri: String,
        uriType: RemoteSetUrlCommand.UriType
    ) = jgit(repositoryPath) {
        remoteSetUrl()
            .setRemoteName(remoteName)
            .setRemoteUri(URIish(uri))
            .setUriType(uriType)
            .call()
    }
}