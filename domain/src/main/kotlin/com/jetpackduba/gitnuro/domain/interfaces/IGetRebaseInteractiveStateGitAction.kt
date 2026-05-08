package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.RebaseInteractiveState
import org.eclipse.jgit.api.Git

interface IGetRebaseInteractiveStateGitAction {
    suspend operator fun invoke(repositoryPath: String): Either<RebaseInteractiveState, GitError>
}