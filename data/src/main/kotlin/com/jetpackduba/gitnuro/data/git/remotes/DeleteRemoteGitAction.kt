package com.jetpackduba.gitnuro.data.git.remotes

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteRemoteGitAction
import javax.inject.Inject

class DeleteRemoteGitAction @Inject constructor(
    private val jgit: JGit,
) : IDeleteRemoteGitAction {
    override suspend operator fun invoke(repositoryPath: String, remoteName: String): Either<Unit, GitError> {
        return jgit.provide(repositoryPath) { git ->
            git
                .remoteRemove()
                .setRemoteName(remoteName)
                .call()
        }
    }
}