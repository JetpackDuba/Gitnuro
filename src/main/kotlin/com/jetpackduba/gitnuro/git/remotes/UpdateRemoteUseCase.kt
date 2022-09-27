package com.jetpackduba.gitnuro.git.remotes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RemoteSetUrlCommand
import org.eclipse.jgit.transport.URIish
import javax.inject.Inject

class UpdateRemoteUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, remoteName: String, uri: String, uriType: RemoteSetUrlCommand.UriType): Unit =
        withContext(Dispatchers.IO) {
            git.remoteSetUrl()
                .setRemoteName(remoteName)
                .setRemoteUri(URIish(uri))
                .setUriType(uriType)
                .call()
        }
}