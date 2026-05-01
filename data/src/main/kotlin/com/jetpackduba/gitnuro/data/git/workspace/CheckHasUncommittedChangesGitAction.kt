package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.extensions.hasUntrackedChanges
import com.jetpackduba.gitnuro.domain.interfaces.ICheckHasUncommittedChangesGitAction
import javax.inject.Inject

class CheckHasUncommittedChangesGitAction @Inject constructor(
    private val jgit: JGit,
) : ICheckHasUncommittedChangesGitAction {
    override suspend operator fun invoke(repositoryPath: String): Either<Boolean, GitError> = jgit.provide(repositoryPath) { git ->
        val status = git
            .status()
            .call()

        status.hasUncommittedChanges() || status.hasUntrackedChanges()
    }
}