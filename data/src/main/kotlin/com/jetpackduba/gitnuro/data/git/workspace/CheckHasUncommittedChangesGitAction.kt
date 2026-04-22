package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.extensions.hasUntrackedChanges
import com.jetpackduba.gitnuro.domain.interfaces.ICheckHasUncommittedChangesGitAction
import javax.inject.Inject

class CheckHasUncommittedChangesGitAction @Inject constructor() : ICheckHasUncommittedChangesGitAction {
    override suspend operator fun invoke(repositoryPath: String): Either<Boolean, GitError> = jgit(repositoryPath) {
        val status = this
            .status()
            .call()

        status.hasUncommittedChanges() || status.hasUntrackedChanges()
    }
}