package com.jetpackduba.gitnuro.data.git.remotes

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.IAddRemoteGitAction
import org.eclipse.jgit.transport.URIish
import javax.inject.Inject

class AddRemoteGitAction @Inject constructor(
    private val jgit: JGit,
) : IAddRemoteGitAction {
    override suspend operator fun invoke(
        repositoryPath: String,
        remoteName: String,
        fetchUri: String,
    ) = jgit.provide(repositoryPath) { git ->
        git
            .remoteAdd()
            .setName(remoteName)
            .setUri(URIish(fetchUri))
            .call()

        Unit
    }
}