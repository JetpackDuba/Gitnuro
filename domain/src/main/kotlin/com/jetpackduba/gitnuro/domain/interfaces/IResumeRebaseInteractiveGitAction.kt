package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import org.eclipse.jgit.api.RebaseCommand

interface IResumeRebaseInteractiveGitAction {
    suspend operator fun invoke(repositoryPath: String, interactiveHandler: RebaseCommand.InteractiveHandler): Either<Unit, GitError>
}