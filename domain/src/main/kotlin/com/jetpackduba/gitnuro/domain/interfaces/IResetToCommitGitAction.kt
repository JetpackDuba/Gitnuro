package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.usecases.ResetType
import org.eclipse.jgit.api.Git

interface IResetToCommitGitAction {
    suspend operator fun invoke(repositoryPath: String, commit: Commit, resetType: ResetType): Either<Unit, GitError>
}