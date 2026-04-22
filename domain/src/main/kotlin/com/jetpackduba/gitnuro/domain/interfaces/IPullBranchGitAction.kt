package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.PullType
import org.eclipse.jgit.api.Git

interface IPullBranchGitAction {
    suspend operator fun invoke(
        repositoryPath: String,
        pullType: PullType,
        mergeAutoStash: Boolean = true, // TODO Fix this after refactor
    ): Either<PullHasConflicts, GitError>
}