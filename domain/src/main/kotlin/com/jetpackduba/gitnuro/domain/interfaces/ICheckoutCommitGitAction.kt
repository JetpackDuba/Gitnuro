package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.Commit
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit

interface ICheckoutCommitGitAction {
    suspend operator fun invoke(repositoryPath: String, commit: Commit): Either<Unit, GitError>
    suspend operator fun invoke(repositoryPath: String, hash: String): Either<Unit, GitError>
}