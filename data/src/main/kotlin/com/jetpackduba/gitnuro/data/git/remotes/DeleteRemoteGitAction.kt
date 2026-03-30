package com.jetpackduba.gitnuro.data.git.remotes

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteRemoteGitAction
import javax.inject.Inject

class DeleteRemoteGitAction @Inject constructor() : IDeleteRemoteGitAction {
    override suspend operator fun invoke(repositoryPath: String, remoteName: String): Either<Unit, GitError> {
        return jgit(repositoryPath) {
            remoteRemove()
                .setRemoteName(remoteName)
                .call()
        }
    }
}