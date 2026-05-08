package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.Commit

interface IGetCommitFromRebaseLineGitAction {
    suspend operator fun invoke(repositoryPath: String, commitHash: String, shortMessage: String): Either<Commit?, GitError>
}